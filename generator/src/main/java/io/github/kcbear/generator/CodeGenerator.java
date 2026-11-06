package io.github.kcbear.generator;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Purpose: the template engine driver. For every {@link EntitySchema} it renders the
 * FreeMarker templates in src/main/resources/templates into
 *  - Java sources: JPA entity, Spring Data repository, Spring MVC REST controller
 *  - TypeScript sources: react-admin list (TanStack Table), create/edit forms
 * plus one aggregated resources.tsx that registers all generated react-admin resources.
 * Output folders named "generated" are wiped first so removed schemas leave no stale files.
 */
public class CodeGenerator {

    private final Configuration cfg;

    public CodeGenerator() {
        cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassLoaderForTemplateLoading(CodeGenerator.class.getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        DefaultObjectWrapperBuilder wrapper = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_33);
        wrapper.setExposeFields(true); // lets templates read public schema fields
        cfg.setObjectWrapper(wrapper.build());
    }

    /**
     * @param basePackage Java base package of the backend, e.g. io.github.kcbear.trading
     * @param javaOut     source root receiving the generated Java (package folders are created)
     * @param tsOut       folder receiving the generated TypeScript (cleaned only if named "generated")
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
        for (EntitySchema e : entities) {
            Map<String, Object> model = new HashMap<>();
            model.put("entity", e);
            model.put("basePackage", basePackage);
            written.add(render("Entity.java.ftl", model, pkgRoot.resolve("model").resolve(e.name + ".java")));
            written.add(render("Repository.java.ftl", model,
                    pkgRoot.resolve("repository").resolve(e.name + "Repository.java")));
            written.add(render("Controller.java.ftl", model,
                    pkgRoot.resolve("web").resolve(e.name + "Controller.java")));
            written.add(render("List.tsx.ftl", model, tsOut.resolve(e.name + "List.tsx")));
            written.add(render("Form.tsx.ftl", model, tsOut.resolve(e.name + "Form.tsx")));
        }
        Map<String, Object> all = new HashMap<>();
        all.put("entities", entities);
        written.add(render("resources.tsx.ftl", all, tsOut.resolve("resources.tsx")));
        return written;
    }

    private Path render(String template, Map<String, Object> model, Path out) throws IOException {
        Files.createDirectories(out.getParent());
        try (Writer w = Files.newBufferedWriter(out)) {
            cfg.getTemplate(template).process(model, w);
        } catch (TemplateException ex) {
            throw new IOException("Template " + template + " failed: " + ex.getMessage(), ex);
        }
        return out;
    }

    private static void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
