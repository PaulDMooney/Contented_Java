package com.contented.contented.contentitem.model;

import com.contented.contented.persistence.SchemalessData;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Table("content_item")
public class ContentItemEntity implements Persistable<UUID> {

    public static final String CONTENT_TYPE_FIELD = "contentType";

    @Id
    private UUID id;

    private String contentType;

    private SchemalessData data;

    // Drives Spring Data JDBC's INSERT-vs-UPDATE choice for our assigned (non-generated) ids;
    // create sets it true, update sets it false, and a DB-loaded row is constructed as not-new.
    @Transient
    private boolean isNew = true;

    private ContentItemEntity(UUID id, String contentType, SchemalessData data, boolean isNew) {
        this.id = id;
        this.contentType = contentType;
        this.data = data;
        this.isNew = isNew;
    }

    public ContentItemEntity(UUID id, String contentType, Map<String, Object> schemalessData) {
        this(id, contentType, new SchemalessData(new LinkedHashMap<>(schemalessData)), true);
    }

    // Spring Data instantiates loaded rows here; an existing row is by definition not new.
    @PersistenceCreator
    static ContentItemEntity fromDatabase(UUID id, String contentType, SchemalessData data) {
        return new ContentItemEntity(id, contentType, data, false);
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
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public Map<String, Object> getSchemalessData() {
        return data.values();
    }

    public <T> T get(String key) {
        return data.get(key);
    }
}
