package com.contented.contented.contentlet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wrapper around the schemaless field map so Spring Data JDBC maps it to a single {@code jsonb}
 * column via a converter targeting this type, rather than treating a raw {@code Map} as a child
 * relation. Equality is value-based on the backing map.
 */
public record SchemalessData(Map<String, Object> values) {

    public SchemalessData() {
        this(new LinkedHashMap<>());
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }
}
