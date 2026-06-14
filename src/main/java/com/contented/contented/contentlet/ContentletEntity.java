package com.contented.contented.contentlet;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Table("contentlet")
public class ContentletEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @JsonIgnore
    private SchemalessData data;

    // Drives Spring Data JDBC's INSERT-vs-UPDATE choice for our assigned (non-generated) ids;
    // set from the existsById check in ContentletService.
    @Transient
    @JsonIgnore
    private boolean isNew = true;

    public ContentletEntity() {
        this.data = new SchemalessData();
    }

    public ContentletEntity(UUID id) {
        this(id, new LinkedHashMap<>());
    }

    public ContentletEntity(UUID id, Map<String, Object> schemalessData) {
        this.id = id;
        this.data = new SchemalessData(new LinkedHashMap<>(schemalessData));
    }

    @PersistenceCreator
    ContentletEntity(UUID id, SchemalessData data) {
        this.id = id;
        this.data = data;
        this.isNew = false;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    @JsonAnySetter
    public void add(String key, Object value) {
        data.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getSchemalessData() {
        return data.values();
    }

    public <T> T get(String key) {
        return data.get(key);
    }
}
