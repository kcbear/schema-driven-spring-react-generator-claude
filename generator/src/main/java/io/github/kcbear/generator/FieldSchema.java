package io.github.kcbear.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose: in-memory model of ONE field of an entity schema (one entry of the "fields"
 * array in a schemas/*.json file). Jackson fills the public properties from JSON; the
 * getters expose derived values (Java type, JPA/validation annotations, react-admin
 * input component...) that the FreeMarker templates consume.
 */
public class FieldSchema {

    /** Supported schema types. */
    public static final List<String> TYPES =
            List.of("string", "long", "integer", "decimal", "boolean", "date", "datetime", "enum");

    public String name;
    /** One of {@link #TYPES}. */
    public String type;
    public String label;
    public String description;
    public boolean primaryKey;
    public boolean generated;
    public boolean required;
    /** String length (0 = default 255). */
    public int maxLength;
    public int precision = 19;
    public int scale = 4;
    /** Column can be sorted from the table header. */
    public boolean sortable = true;
    /** A filter input is generated for this column. */
    public boolean filterable;
    /** Included in the free-text "q" search box. */
    public boolean searchable;
    /** Shown as a column in the list table. */
    public boolean listVisible = true;
    /** Allowed values when type is "enum". */
    public List<String> values = new ArrayList<>();

    public String getCapName() {
        return Naming.capitalize(name);
    }

    public String getColumnName() {
        return Naming.snake(name);
    }

    public String getDisplayLabel() {
        return label != null ? label : Naming.humanize(name);
    }

    /** Java type used in the generated JPA entity. */
    public String getJavaType() {
        return switch (type) {
            case "long" -> "Long";
            case "integer" -> "Integer";
            case "decimal" -> "BigDecimal";
            case "boolean" -> "Boolean";
            case "date" -> "LocalDate";
            case "datetime" -> "Instant";
            default -> "String"; // string + enum
        };
    }

    /** Rendering hint understood by the generic TanStack table component. */
    public String getTsKind() {
        return switch (type) {
            case "string" -> "text";
            case "long", "integer", "decimal" -> "number";
            default -> type; // boolean, date, datetime, enum
        };
    }

    /** react-admin input component used in the generated create/edit forms. */
    public String getRaInput() {
        return switch (type) {
            case "long", "integer", "decimal" -> "NumberInput";
            case "boolean" -> "BooleanInput";
            case "date" -> "DateInput";
            case "datetime" -> "DateTimeInput";
            case "enum" -> "SelectInput";
            default -> "TextInput";
        };
    }

    private int stringLength() {
        return "enum".equals(type) ? 32 : (maxLength > 0 ? maxLength : 255);
    }

    /** JPA + Bean Validation annotations to emit above the Java field. */
    public List<String> getAnnotations() {
        List<String> a = new ArrayList<>();
        if (primaryKey) {
            a.add("@Id");
            if (generated) {
                a.add("@GeneratedValue(strategy = GenerationType.IDENTITY)");
            }
        } else if (required) {
            a.add(("string".equals(type) || "enum".equals(type)) ? "@NotBlank" : "@NotNull");
        }
        if ("string".equals(type)) {
            a.add("@Size(max = " + stringLength() + ")");
        }
        if ("enum".equals(type)) {
            a.add("@Pattern(regexp = \"" + String.join("|", values) + "\")");
        }
        StringBuilder col = new StringBuilder("@Column(name = \"" + getColumnName() + "\"");
        if (required || primaryKey) {
            col.append(", nullable = false");
        }
        if ("string".equals(type) || "enum".equals(type)) {
            col.append(", length = ").append(stringLength());
        }
        if ("decimal".equals(type)) {
            col.append(", precision = ").append(precision).append(", scale = ").append(scale);
        }
        a.add(col.append(")").toString());
        return a;
    }
}
