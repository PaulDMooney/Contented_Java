package com.contented.contented.contentitem.model;

import com.contented.contented.persistence.SchemalessData;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Table("content_item")
public class ContentItemEntity implements Persistable<UUID> {

    public static final String CONTENT_TYPE_FIELD = "contentType";

    // Identifies this exact version row (the primary key).
    @Id
    private UUID versionId;

    // The version-agnostic id grouping the working/live/archived versions of one logical content.
    private UUID identifier;

    private String contentType;

    private ContentItemState state;

    // When this version came into existence; immutable for the life of the version.
    private Instant versionCreatedDatetime;

    private SchemalessData data;

    // Drives Spring Data JDBC's INSERT-vs-UPDATE choice for our assigned (non-generated) ids;
    // a new version sets it true, an in-place edit/state-flip sets it false, and a DB-loaded row
    // is constructed as not-new.
    @Transient
    private boolean isNew = true;

    private ContentItemEntity(UUID versionId, UUID identifier, String contentType, ContentItemState state,
                              Instant versionCreatedDatetime, SchemalessData data, boolean isNew) {
        this.versionId = versionId;
        this.identifier = identifier;
        this.contentType = contentType;
        this.state = state;
        this.versionCreatedDatetime = versionCreatedDatetime;
        this.data = data;
        this.isNew = isNew;
    }

    public ContentItemEntity(UUID versionId, String contentType, Map<String, Object> schemalessData) {
        this(versionId, null, contentType, null, null, new SchemalessData(new LinkedHashMap<>(schemalessData)), true);
    }

    // Spring Data instantiates loaded rows here; an existing row is by definition not new.
    @PersistenceCreator
    static ContentItemEntity fromDatabase(UUID versionId, UUID identifier, String contentType, ContentItemState state,
                                          Instant versionCreatedDatetime, SchemalessData data) {
        return new ContentItemEntity(versionId, identifier, contentType, state, versionCreatedDatetime, data, false);
    }

    /**
     * Builds a fully-populated version, marked new so it is INSERTed. For an in-place update of an
     * existing version (the working draft), call {@link #setNew(boolean)} with {@code false} afterwards.
     */
    public static ContentItemEntity newVersion(UUID versionId, UUID identifier, String contentType,
                                               ContentItemState state, Instant versionCreatedDatetime,
                                               Map<String, Object> schemalessData) {
        return new ContentItemEntity(versionId, identifier, contentType, state, versionCreatedDatetime,
            new SchemalessData(new LinkedHashMap<>(schemalessData)), true);
    }

    public UUID getVersionId() {
        return versionId;
    }

    // The Persistable contract; the version id is the primary key. Marked @Transient so Spring Data
    // does not treat this getter as a second (`id`) persistent property alongside `versionId`.
    @Override
    @Transient
    public UUID getId() {
        return versionId;
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public String getContentType() {
        return contentType;
    }

    public ContentItemState getState() {
        return state;
    }

    public void setState(ContentItemState state) {
        this.state = state;
    }

    public Instant getVersionCreatedDatetime() {
        return versionCreatedDatetime;
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
