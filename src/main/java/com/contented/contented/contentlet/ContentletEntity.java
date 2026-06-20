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

    public static final String CONTENT_TYPE_FIELD = "contentType";

    @Id
    private UUID id;

    private String contentType;

    @JsonIgnore
    private SchemalessData data;

    // Drives Spring Data JDBC's INSERT-vs-UPDATE choice for our assigned (non-generated) ids;
    // create sets it true, update sets it false, and a DB-loaded row is constructed as not-new.
    @Transient
    @JsonIgnore
    private boolean isNew = true;

    private ContentletEntity(UUID id, String contentType, SchemalessData data, boolean isNew) {
        this.id = id;
        this.contentType = contentType;
        this.data = data;
        this.isNew = isNew;
    }

    // Required by Jackson to deserialize a contentlet from JSON: id/contentType land via their
    // setters, everything else via @JsonAnySetter. (Spring Data uses the @PersistenceCreator factory.)
    public ContentletEntity() {
        this(null, null, new SchemalessData(), true);
    }

    public ContentletEntity(UUID id, String contentType, Map<String, Object> schemalessData) {
        this(id, contentType, new SchemalessData(new LinkedHashMap<>(schemalessData)), true);
    }

    // Spring Data instantiates loaded rows here; an existing row is by definition not new.
    @PersistenceCreator
    static ContentletEntity fromDatabase(UUID id, String contentType, SchemalessData data) {
        return new ContentletEntity(id, contentType, data, false);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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
