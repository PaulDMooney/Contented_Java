package com.contented.contented.contentitem.model;

import com.contented.contented.persistence.SchemalessData;
import lombok.Builder;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Table("content_item_version")
public class ContentItemVersionEntity implements Persistable<UUID> {

    public static final String CONTENT_TYPE_FIELD = "contentType";

    // Identifies this exact version row (the primary key).
    @Id
    private UUID versionId;

    // The version-agnostic id grouping the working/live/archived versions of one logical content.
    private UUID identifier;

    private String contentType;

    // A copy carrying a different lifecycle state (e.g. promoting WORKING -> LIVE on publish).
    @With
    private ContentItemState state;

    // When this version came into existence; immutable for the life of the version.
    private Instant versionCreatedDatetime;

    private SchemalessData data;

    // Drives Spring Data JDBC's INSERT-vs-UPDATE choice for our assigned (non-generated) ids;
    // a new version is new (INSERT), and a DB-loaded row (or a copy of one) is not (UPDATE).
    @Transient
    private boolean isNew;

    @Builder(toBuilder = true)
    private ContentItemVersionEntity(UUID versionId, UUID identifier, String contentType, ContentItemState state,
                              Instant versionCreatedDatetime, SchemalessData data, boolean isNew) {
        this.versionId = versionId;
        this.identifier = identifier;
        this.contentType = contentType;
        this.state = state;
        this.versionCreatedDatetime = versionCreatedDatetime;
        this.data = data;
        this.isNew = isNew;
    }

    public ContentItemVersionEntity(UUID versionId, String contentType, Map<String, Object> data) {
        this(versionId, null, contentType, null, null, new SchemalessData(new LinkedHashMap<>(data)), true);
    }

    // Spring Data instantiates loaded rows here; an existing row is by definition not new.
    @PersistenceCreator
    static ContentItemVersionEntity fromDatabase(UUID versionId, UUID identifier, String contentType, ContentItemState state,
                                          Instant versionCreatedDatetime, SchemalessData data) {
        return new ContentItemVersionEntity(versionId, identifier, contentType, state, versionCreatedDatetime, data, false);
    }

    /**
     * Builds a fully-populated new version, marked new so it is INSERTed.
     */
    public static ContentItemVersionEntity newVersion(UUID versionId, UUID identifier, String contentType,
                                               ContentItemState state, Instant versionCreatedDatetime,
                                               Map<String, Object> data) {
        return new ContentItemVersionEntity(versionId, identifier, contentType, state, versionCreatedDatetime,
            new SchemalessData(new LinkedHashMap<>(data)), true);
    }

    /**
     * Promotes this entity — typically the output of the inbound transformation pipeline — into a
     * freshly-identified new version, keeping its own {@code contentType} and content. Marked new so
     * it is INSERTed.
     */
    public ContentItemVersionEntity asNewVersion(UUID versionId, UUID identifier, ContentItemState state,
                                          Instant versionCreatedDatetime) {
        return asNewVersion(versionId, identifier, contentType, state, versionCreatedDatetime);
    }

    /**
     * As {@link #asNewVersion(UUID, UUID, ContentItemState, Instant)}, but with {@code contentType}
     * given explicitly rather than taken from this entity — for lazily creating a working version
     * whose contentType must match an already-established one exactly (e.g. its casing), not
     * whatever a client happened to submit.
     */
    public ContentItemVersionEntity asNewVersion(UUID versionId, UUID identifier, String contentType,
                                          ContentItemState state, Instant versionCreatedDatetime) {
        return toBuilder()
            .versionId(versionId)
            .identifier(identifier)
            .contentType(contentType)
            .state(state)
            .versionCreatedDatetime(versionCreatedDatetime)
            .isNew(true)
            .build();
    }

    /**
     * A copy of this version carrying replacement schemaless content — an in-place edit of the
     * working draft. The version id and creation date are kept and the copy is marked not-new, so it
     * UPDATEs the existing row rather than inserting a new one.
     */
    public ContentItemVersionEntity withData(Map<String, Object> data) {
        return toBuilder()
            .data(new SchemalessData(new LinkedHashMap<>(data)))
            .isNew(false)
            .build();
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

    public Instant getVersionCreatedDatetime() {
        return versionCreatedDatetime;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Map<String, Object> getData() {
        return data.values();
    }

    public <T> T get(String key) {
        return data.get(key);
    }
}
