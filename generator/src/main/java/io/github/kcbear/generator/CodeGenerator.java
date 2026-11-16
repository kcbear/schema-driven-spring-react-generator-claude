package io.github.kcbear.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

/**
 * Purpose: generates backend Java sources with JavaPoet and frontend TSX sources with
 * explicit Java source builders. The schema model remains the single source of truth.
 * Generated Java includes JPA entities, Spring Data repositories and REST controllers;
 * generated TypeScript includes react-admin lists, forms and the resource registry.
 * Output folders named "generated" are cleaned first so removed schemas leave no stale files.
 */
public class CodeGenerator {

    private static final ClassName ENTITY = ClassName.get("jakarta.persistence", "Entity");
    private static final ClassName TABLE = ClassName.get("jakarta.persistence", "Table");
    private static final ClassName COLUMN = ClassName.get("jakarta.persistence", "Column");
    private static final ClassName ID = ClassName.get("jakarta.persistence", "Id");
    private static final ClassName GENERATED_VALUE = ClassName.get("jakarta.persistence", "GeneratedValue");
    private static final ClassName GENERATION_TYPE = ClassName.get("jakarta.persistence", "GenerationType");
    private static final ClassName NOT_NULL = ClassName.get("jakarta.validation.constraints", "NotNull");
    private static final ClassName NOT_BLANK = ClassName.get("jakarta.validation.constraints", "NotBlank");
    private static final ClassName SIZE = ClassName.get("jakarta.validation.constraints", "Size");
    private static final ClassName PATTERN = ClassName.get("jakarta.validation.constraints", "Pattern");
    private static final ClassName JPA_REPOSITORY = ClassName.get("org.springframework.data.jpa.repository", "JpaRepository");
    private static final ClassName SPECIFICATION_EXECUTOR = ClassName.get("org.springframework.data.jpa.repository", "JpaSpecificationExecutor");

    /**
     * @param basePackage Java base package of the backend, e.g. io.github.kcbear.trading
     * @param javaOut source root receiving generated Java package folders
     * @param tsOut folder receiving generated TypeScript (cleaned only if named "generated")
     * @return all files written
     */
    public List<Path> generate(List<EntitySchema> entities, String basePackage, Path javaOut, Path tsOut)
            throws IOException {
        List<Path> written = new ArrayList<>();
        Path pkgRoot = javaOut.resolve(basePackage.replace('.', '/')).resolve("generated");
        cleanDirectory(pkgRoot);
        if ("generated".equals(tsOut.getFileName().toString())) {
            cleanDirectory(tsOut);
        }
        for (EntitySchema entity : entities) {
            written.add(writeJava(buildEntity(entity, basePackage), pkgRoot.resolve("model").resolve(entity.name + ".java")));
            written.add(writeJava(buildRepository(entity, basePackage), pkgRoot.resolve("repository").resolve(entity.name + "Repository.java")));
            written.add(writeJava(buildController(entity, basePackage), pkgRoot.resolve("web").resolve(entity.name + "Controller.java")));
            written.add(writeText(tsOut.resolve(entity.name + "List.tsx"), buildList(entity)));
            written.add(writeText(tsOut.resolve(entity.name + "Form.tsx"), buildForm(entity)));
        }
        written.add(writeText(tsOut.resolve("resources.tsx"), buildResources(entities)));
        return written;
    }

    private JavaFile buildEntity(EntitySchema entity, String basePackage) {
        TypeSpec.Builder type = TypeSpec.classBuilder(entity.name).addModifiers(Modifier.PUBLIC)
                .addAnnotation(ENTITY)
                .addAnnotation(AnnotationSpec.builder(TABLE).addMember("name", "$S", entity.getTableName()).build())
                .addJavadoc("Purpose: JPA entity for $L.\nGenerated from schemas/$L; edit the schema instead.\n",
                        entity.label, entity.schemaFile);
        for (FieldSchema field : entity.fields) {
            FieldSpec.Builder property = FieldSpec.builder(javaType(field), field.name, Modifier.PRIVATE);
            if (field.description != null && !field.description.isBlank()) {
                property.addJavadoc("$L\n", field.description);
            }
            addFieldAnnotations(property, field);
            type.addField(property.build());
            String cap = field.getCapName();
            type.addMethod(MethodSpec.methodBuilder("get" + cap).addModifiers(Modifier.PUBLIC)
                    .returns(javaType(field)).addStatement("return $N", field.name).build());
            type.addMethod(MethodSpec.methodBuilder("set" + cap).addModifiers(Modifier.PUBLIC)
                    .addParameter(javaType(field), field.name)
                    .addStatement("this.$N = $N", field.name, field.name).build());
        }
        return JavaFile.builder(basePackage + ".generated.model", type.build()).indent("    ").build();
    }

    private void addFieldAnnotations(FieldSpec.Builder property, FieldSchema field) {
        if (field.primaryKey) {
            property.addAnnotation(ID);
            if (field.generated) {
                property.addAnnotation(AnnotationSpec.builder(GENERATED_VALUE)
                        .addMember("strategy", "$T.IDENTITY", GENERATION_TYPE).build());
            }
        } else if (field.required) {
            property.addAnnotation(("string".equals(field.type) || "enum".equals(field.type)) ? NOT_BLANK : NOT_NULL);
        }
        if ("string".equals(field.type)) {
            property.addAnnotation(AnnotationSpec.builder(SIZE)
                    .addMember("max", "$L", field.maxLength > 0 ? field.maxLength : 255).build());
        }
        if ("enum".equals(field.type)) {
            property.addAnnotation(AnnotationSpec.builder(PATTERN)
                    .addMember("regexp", "$S", String.join("|", field.values)).build());
        }
        AnnotationSpec.Builder column = AnnotationSpec.builder(COLUMN)
                .addMember("name", "$S", field.getColumnName());
        if (field.required || field.primaryKey) column.addMember("nullable", "$L", false);
        if ("string".equals(field.type) || "enum".equals(field.type)) {
            column.addMember("length", "$L", "enum".equals(field.type) ? 32 : (field.maxLength > 0 ? field.maxLength : 255));
        }
        if ("decimal".equals(field.type)) {
            column.addMember("precision", "$L", field.precision).addMember("scale", "$L", field.scale);
        }
        property.addAnnotation(column.build());
    }

    private JavaFile buildRepository(EntitySchema entity, String basePackage) {
        ClassName model = ClassName.get(basePackage + ".generated.model", entity.name);
        TypeName idType = javaType(entity.getIdField());
        TypeSpec repository = TypeSpec.interfaceBuilder(entity.name + "Repository")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(JPA_REPOSITORY, model, idType))
                .addSuperinterface(ParameterizedTypeName.get(SPECIFICATION_EXECUTOR, model))
                .addJavadoc("Purpose: Spring Data repository for $L with dynamic filtering, sorting and paging.\nGenerated from schemas/$L.\n",
                        entity.name, entity.schemaFile)
                .build();
        return JavaFile.builder(basePackage + ".generated.repository", repository).indent("    ").build();
    }

    private JavaFile buildController(EntitySchema entity, String basePackage) {
        ClassName model = ClassName.get(basePackage + ".generated.model", entity.name);
        ClassName repository = ClassName.get(basePackage + ".generated.repository", entity.name + "Repository");
        TypeName listModel = ParameterizedTypeName.get(ClassName.get(List.class), model);
        ClassName simpleRest = ClassName.get(basePackage + ".web", "SimpleRestSupport");
        ClassName response = ClassName.get("org.springframework.http", "ResponseEntity");
        TypeSpec.Builder type = TypeSpec.classBuilder(entity.name + "Controller").addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                        .addMember("value", "$S", "/api/" + entity.plural).build())
                .addJavadoc("Purpose: simple-REST controller for $L at /api/$L. Generated from schemas/$L.\n",
                        entity.label, entity.plural, entity.schemaFile);

        CodeBlock.Builder config = CodeBlock.builder().add("new $T.Config($S, ", simpleRest, entity.plural)
                .add("$T.of($L), ", ClassName.get(Set.class), quotedFields(entity.fields.stream().filter(x -> x.sortable).map(x -> x.name).toList()))
                .add("$T.of($L), ", ClassName.get(Set.class), quotedFields(entity.fields.stream().filter(x -> x.filterable || x.primaryKey).map(x -> x.name).toList()))
                .add("$T.of($L), ", ClassName.get(Set.class), quotedFields(entity.fields.stream().filter(x -> "enum".equals(x.type) || x.primaryKey).map(x -> x.name).toList()))
                .add("$T.of($L))", ClassName.get(Set.class), quotedFields(entity.fields.stream().filter(x -> x.searchable).map(x -> x.name).toList()));
        type.addField(FieldSpec.builder(ClassName.get(simpleRest.packageName(), "SimpleRestSupport", "Config"),
                "CONFIG", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer(config.build()).build());
        type.addField(FieldSpec.builder(repository, "repository", Modifier.PRIVATE, Modifier.FINAL).build());
        type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(repository, "repository")
                .addStatement("this.repository = repository").build());

        ClassName getMapping = ClassName.get("org.springframework.web.bind.annotation", "GetMapping");
        ClassName requestParam = ClassName.get("org.springframework.web.bind.annotation", "RequestParam");
        MethodSpec.Builder list = MethodSpec.methodBuilder("list").addModifiers(Modifier.PUBLIC)
                .addAnnotation(getMapping).returns(ParameterizedTypeName.get(response, listModel))
                .addParameter(ParameterSpec.builder(String.class, "sort").addAnnotation(optionalRequestParam(requestParam)).build())
                .addParameter(ParameterSpec.builder(String.class, "range").addAnnotation(optionalRequestParam(requestParam)).build())
                .addParameter(ParameterSpec.builder(String.class, "filter").addAnnotation(optionalRequestParam(requestParam)).build())
                .addStatement("return $T.list(repository, CONFIG, sort, range, filter, $S)",
                        simpleRest, entity.getIdField().name)
                .addJavadoc("Paged, sorted and filtered list; total count is returned in Content-Range.\n");
        type.addMethod(list.build());

        ClassName pathVariable = ClassName.get("org.springframework.web.bind.annotation", "PathVariable");
        ClassName statusException = ClassName.get("org.springframework.web.server", "ResponseStatusException");
        ClassName httpStatus = ClassName.get("org.springframework.http", "HttpStatus");
        type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC).addAnnotation(getMappingPath())
                .addParameter(ParameterSpec.builder(javaType(entity.getIdField()), "id").addAnnotation(pathVariable).build())
                .returns(model).addStatement("return repository.findById(id).orElseThrow(() -> new $T($T.NOT_FOUND))",
                        statusException, httpStatus).build());

        ClassName postMapping = ClassName.get("org.springframework.web.bind.annotation", "PostMapping");
        ClassName requestBody = ClassName.get("org.springframework.web.bind.annotation", "RequestBody");
        ClassName valid = ClassName.get("jakarta.validation", "Valid");
        MethodSpec.Builder create = MethodSpec.methodBuilder("create").addModifiers(Modifier.PUBLIC).addAnnotation(postMapping)
                .addParameter(ParameterSpec.builder(model, "body").addAnnotation(valid).addAnnotation(requestBody).build())
                .returns(model);
        if (entity.getIdField().generated) create.addStatement("body.set$L(null)", entity.getIdField().getCapName());
        create.addStatement("return repository.save(body)");
        type.addMethod(create.build());

        ClassName putMapping = ClassName.get("org.springframework.web.bind.annotation", "PutMapping");
        type.addMethod(MethodSpec.methodBuilder("update").addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(putMapping).addMember("value", "$S", "/{id}").build())
                .addParameter(ParameterSpec.builder(javaType(entity.getIdField()), "id").addAnnotation(pathVariable).build())
                .addParameter(ParameterSpec.builder(model, "body").addAnnotation(valid).addAnnotation(requestBody).build())
                .returns(model).beginControlFlow("if (!repository.existsById(id))")
                .addStatement("throw new $T($T.NOT_FOUND)", statusException, httpStatus).endControlFlow()
                .addStatement("body.set$L(id)", entity.getIdField().getCapName())
                .addStatement("return repository.save(body)").build());

        ClassName deleteMapping = ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping");
        type.addMethod(MethodSpec.methodBuilder("delete").addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(deleteMapping).addMember("value", "$S", "/{id}").build())
                .addParameter(ParameterSpec.builder(javaType(entity.getIdField()), "id").addAnnotation(pathVariable).build())
                .returns(model).addStatement("$T existing = get(id)", model)
                .addStatement("repository.delete(existing)").addStatement("return existing").build());
        return JavaFile.builder(basePackage + ".generated.web", type.build()).indent("    ").build();
    }

    private AnnotationSpec optionalRequestParam(ClassName requestParam) {
        return AnnotationSpec.builder(requestParam).addMember("required", "$L", false).build();
    }

    private AnnotationSpec getMappingPath() {
        return AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                .addMember("value", "$S", "/{id}").build();
    }

    private CodeBlock quotedFields(List<String> fields) {
        CodeBlock.Builder code = CodeBlock.builder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) code.add(", ");
            code.add("$S", fields.get(i));
        }
        return code.build();
    }

    private TypeName javaType(FieldSchema field) {
        return switch (field.type) {
            case "long" -> ClassName.get(Long.class);
            case "integer" -> ClassName.get(Integer.class);
            case "decimal" -> ClassName.get("java.math", "BigDecimal");
            case "boolean" -> ClassName.get(Boolean.class);
            case "date" -> ClassName.get("java.time", "LocalDate");
            case "datetime" -> ClassName.get("java.time", "Instant");
            default -> ClassName.get(String.class);
        };
    }

    private String buildList(EntitySchema entity) {
        StringBuilder out = new StringBuilder();
        out.append("/** Generated react-admin list for ").append(entity.label).append(". Do not edit by hand. */\n")
                .append("import { List, TextInput, SelectInput, BooleanInput } from 'react-admin';\n")
                .append("import { TanStackTable } from '../components/TanStackTable';\n")
                .append("import type { ColumnSpec } from '../components/TanStackTable';\n\n")
                .append("const columns: ColumnSpec[] = [\n");
        entity.fields.stream().filter(f -> f.listVisible).forEach(f -> out.append("  { key: ").append(ts(f.name))
                .append(", label: ").append(ts(f.getDisplayLabel())).append(", kind: ").append(ts(f.getTsKind()))
                .append(", sortable: ").append(f.sortable).append(" },\n"));
        out.append("];\n\nconst filters = [\n");
        if (entity.fields.stream().anyMatch(f -> f.searchable)) {
            out.append("  <TextInput key=\"q\" source=\"q\" label=\"Search\" alwaysOn resettable />,\n");
        }
        for (FieldSchema f : entity.fields) {
            if (!f.filterable || f.primaryKey) continue;
            if ("enum".equals(f.type)) {
                out.append("  <SelectInput key=").append(ts(f.name)).append(" source=").append(ts(f.name))
                        .append(" label=").append(ts(f.getDisplayLabel())).append(" alwaysOn emptyText=\"All\" choices={[")
                        .append(f.values.stream().map(v -> "{ id: " + ts(v) + ", name: " + ts(v) + " }").reduce((a,b) -> a + ", " + b).orElse(""))
                        .append("]} />,\n");
            } else if ("boolean".equals(f.type)) {
                out.append("  <BooleanInput key=").append(ts(f.name)).append(" source=").append(ts(f.name))
                        .append(" label=").append(ts(f.getDisplayLabel())).append(" />,\n");
            } else {
                out.append("  <TextInput key=").append(ts(f.name)).append(" source=").append(ts(f.name))
                        .append(" label=").append(ts(f.getDisplayLabel())).append(" />,\n");
            }
        }
        out.append("];\n\nexport const ").append(entity.name).append("List = () => (\n")
                .append("  <List resource=").append(ts(entity.plural)).append(" title=").append(ts(entity.label))
                .append(" perPage={25} pagination={false} filters={filters} sort={{ field: ")
                .append(ts(entity.getDefaultSortField())).append(", order: 'ASC' }}>\n")
                .append("    <TanStackTable columns={columns} resource=").append(ts(entity.plural)).append(" />\n")
                .append("  </List>\n);\n");
        return out.toString();
    }

    private String buildForm(EntitySchema entity) {
        StringBuilder out = new StringBuilder();
        out.append("/** Generated react-admin forms for ").append(entity.label).append(". Do not edit by hand. */\n")
                .append("import { Create, Edit, SimpleForm, TextInput, NumberInput, BooleanInput, DateInput, DateTimeInput, SelectInput, required } from 'react-admin';\n\n")
                .append("const ").append(entity.name).append("Inputs = () => (\n  <>\n");
        for (FieldSchema f : entity.fields) {
            if (f.primaryKey) continue;
            String component = f.getRaInput();
            out.append("    <").append(component).append(" source=").append(ts(f.name))
                    .append(" label=").append(ts(f.getDisplayLabel()));
            if ("enum".equals(f.type)) {
                out.append(" choices={[")
                        .append(f.values.stream().map(v -> "{ id: " + ts(v) + ", name: " + ts(v) + " }").reduce((a,b) -> a + ", " + b).orElse(""))
                        .append("]}");
            }
            if (f.required) out.append(" validate={required()}");
            out.append(" />\n");
        }
        out.append("  </>\n);\n\nexport const ").append(entity.name).append("Edit = () => (\n")
                .append("  <Edit><SimpleForm><TextInput source=").append(ts(entity.getIdField().name))
                .append(" disabled /><").append(entity.name).append("Inputs /></SimpleForm></Edit>\n);\n\n")
                .append("export const ").append(entity.name).append("Create = () => (\n")
                .append("  <Create><SimpleForm><").append(entity.name).append("Inputs /></SimpleForm></Create>\n);\n");
        return out.toString();
    }

    private String buildResources(List<EntitySchema> entities) {
        StringBuilder out = new StringBuilder("/** Generated react-admin resource registry. Do not edit by hand. */\nimport { Resource } from 'react-admin';\n");
        for (EntitySchema e : entities) {
            out.append("import { ").append(e.name).append("List } from './").append(e.name).append("List';\n")
                    .append("import { ").append(e.name).append("Edit, ").append(e.name).append("Create } from './")
                    .append(e.name).append("Form';\n");
        }
        out.append("\nexport const generatedResources = [\n");
        for (EntitySchema e : entities) {
            out.append("  <Resource key=").append(ts(e.plural)).append(" name=").append(ts(e.plural))
                    .append(" options={{ label: ").append(ts(e.label)).append(" }} list={").append(e.name)
                    .append("List} edit={").append(e.name).append("Edit} create={").append(e.name).append("Create} />,\n");
        }
        return out.append("];\n").toString();
    }

    private String ts(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    private Path writeJava(JavaFile source, Path out) throws IOException {
        Files.createDirectories(out.getParent());
        source.writeTo(out.getParent().getParent().getParent());
        return out;
    }

    private Path writeText(Path out, String content) throws IOException {
        Files.createDirectories(out.getParent());
        Files.writeString(out, content);
        return out;
    }

    private static void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
