package io.github.kcbear.generator;

import java.util.Locale;

/**
 * Purpose: small, dependency-free string helpers that convert schema identifiers
 * (camelCase) into the other spellings the templates need: snake_case for SQL
 * columns, "Title Case" for UI labels and Capitalised for Java accessors.
 */
final class Naming {

    private Naming() {
    }

    /** instrumentType -> instrument_type */
    static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    /** instrumentType -> Instrument Type */
    static String humanize(String camel) {
        String spaced = camel.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return capitalize(spaced);
    }

    /** instrumentType -> InstrumentType */
    static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
