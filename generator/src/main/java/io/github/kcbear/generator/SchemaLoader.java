package io.github.kcbear.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Purpose: reads schema JSON files into {@link EntitySchema} objects, applies defaults
 * (plural, label, description) and validates them so that mistakes in a schema fail fast
 * with a clear message instead of producing code that does not compile.
 */
public class SchemaLoader {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Loads every *.json file in the directory (sorted by file name). */
    public List<EntitySchema> loadAll(Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No *.json schemas found in " + dir);
        }
        return files.stream().map(p -> {
            try {
                return load(p);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + p, e);
            }
        }).toList();
    }

    /** Loads and validates a single schema file. */
    public EntitySchema load(Path file) throws IOException {
        EntitySchema e = mapper.readValue(file.toFile(), EntitySchema.class);
        e.schemaFile = file.getFileName().toString();
        applyDefaults(e);
        validate(e);
        return e;
    }

    private static void applyDefaults(EntitySchema e) {
        if (e.plural == null && e.name != null) {
            e.plural = e.name.toLowerCase() + "s";
        }
        if (e.label == null && e.plural != null) {
            e.label = Naming.humanize(e.plural);
        }
        if (e.description == null) {
            e.description = e.label;
        }
    }

    /** Throws IllegalArgumentException describing the first problem found. */
    static void validate(EntitySchema e) {
        String where = "Schema " + e.schemaFile + ": ";
        require(e.name != null && e.name.matches("[A-Z][A-Za-z0-9]*"), where + "name must be a Java class name");
        require(e.plural.matches("[a-z][a-z0-9]*"), where + "plural must be lowercase letters/digits");
        require(e.fields != null && !e.fields.isEmpty(), where + "at least one field is required");
        Set<String> seen = new HashSet<>();
        int keys = 0;
        for (FieldSchema f : e.fields) {
            require(f.name != null && f.name.matches("[a-z][A-Za-z0-9]*"), where + "invalid field name " + f.name);
            require(seen.add(f.name), where + "duplicate field " + f.name);
            require(FieldSchema.TYPES.contains(f.type), where + "field " + f.name + " has unknown type " + f.type);
            if ("enum".equals(f.type)) {
                require(!f.values.isEmpty() && f.values.stream().allMatch(v -> v.matches("[A-Z0-9_]+")),
                        where + "enum field " + f.name + " needs UPPER_CASE values");
            }
            if (f.primaryKey) {
                keys++;
                require("id".equals(f.name), where + "the primary key must be named 'id' (react-admin convention)");
            }
        }
        require(keys == 1, where + "exactly one primaryKey field is required");
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new IllegalArgumentException(message);
        }
    }
}
