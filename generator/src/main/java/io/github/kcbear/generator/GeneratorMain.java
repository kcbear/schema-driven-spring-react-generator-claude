package io.github.kcbear.generator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Purpose: command line entry point of the generator, invoked by the Gradle task
 * ":backend:generateCode". Usage:
 * --schemas DIR --java-out DIR --ts-out DIR --package io.github.kcbear.trading
 */
public final class GeneratorMain {

    private GeneratorMain() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            opts.put(args[i], args[i + 1]);
        }
        Path schemas = Path.of(required(opts, "--schemas"));
        Path javaOut = Path.of(required(opts, "--java-out"));
        Path tsOut = Path.of(required(opts, "--ts-out"));
        String pkg = required(opts, "--package");

        List<EntitySchema> entities = new SchemaLoader().loadAll(schemas);
        List<Path> files = new CodeGenerator().generate(entities, pkg, javaOut, tsOut);
        System.out.println("Generated " + files.size() + " files from " + entities.size() + " schemas");
    }

    private static String required(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing argument " + key);
        }
        return v;
    }
}
