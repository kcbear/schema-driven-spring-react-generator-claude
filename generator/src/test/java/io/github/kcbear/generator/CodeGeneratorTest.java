package io.github.kcbear.generator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Purpose: unit tests for the generator - checks that a small schema produces the expected
 * Java and TypeScript files, and that invalid schemas are rejected with an exception.
 */
class CodeGeneratorTest {

    private static final String WIDGET = """
            { "name": "Widget", "plural": "widgets", "description": "Test widget",
              "fields": [
                { "name": "id", "type": "long", "primaryKey": true, "generated": true },
                { "name": "title", "type": "string", "required": true, "searchable": true, "filterable": true },
                { "name": "kind", "type": "enum", "values": ["A", "B"], "filterable": true } ] }
            """;

    @Test
    void generatesJavaAndTypeScript(@TempDir Path tmp) throws Exception {
        Path schemas = Files.createDirectories(tmp.resolve("schemas"));
        Files.writeString(schemas.resolve("widget.json"), WIDGET);

        List<EntitySchema> entities = new SchemaLoader().loadAll(schemas);
        new CodeGenerator().generate(entities, "com.acme", tmp.resolve("java"), tmp.resolve("ts/generated"));

        String entity = Files.readString(tmp.resolve("java/com/acme/generated/model/Widget.java"));
        assertTrue(entity.contains("@Entity") && entity.contains("class Widget"));
        assertTrue(entity.contains("@NotBlank"));
        assertTrue(Files.readString(tmp.resolve("java/com/acme/generated/web/WidgetController.java"))
                .contains("/api/widgets"));
        assertTrue(Files.readString(tmp.resolve("ts/generated/WidgetList.tsx")).contains("TanStackTable"));
        assertTrue(Files.readString(tmp.resolve("ts/generated/resources.tsx")).contains("name=\"widgets\""));
    }

    @Test
    void rejectsSchemaWithoutPrimaryKey(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("bad.json");
        Files.writeString(f, "{ \"name\": \"Bad\", \"fields\": [ { \"name\": \"x\", \"type\": \"string\" } ] }");
        assertThrows(IllegalArgumentException.class, () -> new SchemaLoader().load(f));
    }
}
