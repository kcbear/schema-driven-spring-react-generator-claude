package io.github.kcbear.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose: in-memory model of one schema file (schemas/*.json) describing a business
 * entity: its Java class name, REST resource name, UI label and its fields. This is the
 * single source of truth from which the JPA entity, repository, REST controller and the
 * react-admin / TanStack Table views are generated.
 */
public class EntitySchema {

    /** Java class name, e.g. "Instrument". */
    public String name;
    /** REST resource + react-admin resource name, e.g. "instruments". */
    public String plural;
    /** Human readable title used in the UI. */
    public String label;
    public String description;
    public List<FieldSchema> fields = new ArrayList<>();
    /** Name of the file the schema was loaded from (set by SchemaLoader). */
    public String schemaFile;

    public String getTableName() {
        return Naming.snake(plural);
    }

    public FieldSchema getIdField() {
        return fields.stream().filter(f -> f.primaryKey).findFirst()
                .orElseThrow(() -> new IllegalStateException("No primary key in " + name));
    }

    public String getIdType() {
        return getIdField().getJavaType();
    }

    /** First sortable, list-visible, non-key column; falls back to the id. */
    public String getDefaultSortField() {
        return fields.stream().filter(f -> f.sortable && f.listVisible && !f.primaryKey)
                .map(f -> f.name).findFirst().orElse(getIdField().name);
    }
}
