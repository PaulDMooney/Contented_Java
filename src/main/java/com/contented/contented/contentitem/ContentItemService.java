package com.contented.contented.contentitem;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.events.ContentItemDeletedEvent;
import com.contented.contented.contentitem.events.ContentItemPublishedEvent;
import com.contented.contented.contentitem.exceptions.InvalidContentItemException;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemMapper;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.model.ContentItemState;
import com.contented.contented.contentitem.model.ContentItemWorkAndLiveDTO;
import com.contented.contented.contentitem.model.ContentItemVersionSummaryDTO;
import com.contented.contented.contentitem.transformation.TransformationHandler;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class ContentItemService {

    private final ContentItemRepository contentItemRepository;

    private final TransformationHandler transformationHandler;

    private final ContentItemMapper contentItemMapper;

    private final ApplicationEventPublisher eventPublisher;

    public ContentItemService(ContentItemRepository contentItemRepository, TransformationHandler transformationHandler,
                              ContentItemMapper contentItemMapper, ApplicationEventPublisher eventPublisher) {
        this.contentItemRepository = contentItemRepository;
        this.transformationHandler = transformationHandler;
        this.contentItemMapper = contentItemMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a brand-new logical content as a WORKING draft. Nothing is live until {@link #publish}
     * is called, so drafts are not indexed.
     */
    @Transactional
    public ContentItemResponseDTO create(ContentItemDTO dto) {
        requireContentType(dto.getContentType());
        var transformed = transformationHandler.applyTransformation(contentItemMapper.toEntity(dto));
        var toSave = ContentItemEntity.newVersion(UuidV7.generate(), UuidV7.generate(),
            transformed.getContentType(), ContentItemState.WORKING, Instant.now(), transformed.getSchemalessData());
        var saved = contentItemRepository.save(toSave);
        log.info("Created working contentItem version `{}` for identifier `{}`", saved.getVersionId(), saved.getIdentifier());
        return contentItemMapper.toResponse(saved);
    }

    /**
     * Edits the WORKING draft of an existing content: updates it in place if one exists, otherwise
     * creates a fresh working version from the body (lazy working creation). Returns empty if the
     * identifier is unknown (404).
     */
    @Transactional
    public Optional<ContentItemResponseDTO> editWorking(UUID identifier, ContentItemDTO dto) {
        requireContentType(dto.getContentType());
        var existingWorking = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);
        // The stored contentType (and the content's existence) can be read from the working version if
        // present, otherwise the live one; a known content always has one or the other (no operation
        // leaves only archived versions), so their absence means the identifier is unknown.
        var reference = existingWorking.or(() ->
            contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.LIVE));
        if (reference.isEmpty()) {
            log.info("No content for identifier `{}`; nothing to edit", identifier);
            return Optional.empty();
        }
        // contentType is fixed across all versions of a content.
        var storedContentType = reference.get().getContentType();
        if (!storedContentType.equalsIgnoreCase(dto.getContentType())) {
            throw new InvalidContentItemException(
                "A contentItem's contentType cannot be changed; `" + storedContentType
                    + "` was created, `" + dto.getContentType() + "` was supplied.");
        }

        var transformed = transformationHandler.applyTransformation(contentItemMapper.toEntity(dto));
        // Preserve the stored contentType so its casing never drifts on edit.
        var toSave = workingVersionToSave(identifier, storedContentType, transformed.getSchemalessData(), existingWorking);
        var saved = contentItemRepository.save(toSave);
        log.info("Saved working contentItem version `{}` for identifier `{}`", saved.getVersionId(), identifier);
        return Optional.of(contentItemMapper.toResponse(saved));
    }

    /**
     * Publishes the WORKING draft: the current LIVE version (if any) is demoted to ARCHIVED, then the
     * working version is promoted to LIVE in place. Returns empty for an unknown identifier (404);
     * throws {@link InvalidContentItemException} (400) when there is nothing to publish. The now-live
     * version is indexed after the transaction commits (see {@code ContentItemIndexingListener}).
     */
    @Transactional
    public Optional<ContentItemResponseDTO> publish(UUID identifier) {
        var working = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);
        if (working.isEmpty()) {
            if (!contentItemRepository.existsByIdentifier(identifier)) {
                log.info("No content for identifier `{}`; nothing to publish", identifier);
                return Optional.empty();
            }
            throw new InvalidContentItemException(
                "There is no working version to publish for `" + identifier + "`.");
        }
        // Demote the current live version first so the one-LIVE-per-identifier invariant always holds.
        // These rows were loaded from the database, so their copies are already not-new and will UPDATE.
        contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.LIVE)
            .ifPresent(live -> contentItemRepository.save(live.withState(ContentItemState.ARCHIVED)));
        var published = contentItemRepository.save(working.get().withState(ContentItemState.LIVE));
        eventPublisher.publishEvent(new ContentItemPublishedEvent(published));
        log.info("Published contentItem version `{}` for identifier `{}`", published.getVersionId(), identifier);
        return Optional.of(contentItemMapper.toResponse(published));
    }

    /**
     * Restores a previous version into the WORKING draft by copying its content (history is never
     * modified). The restored content can then be published. Returns empty (404) if the version is
     * unknown or does not belong to the identifier.
     */
    @Transactional
    public Optional<ContentItemResponseDTO> restore(UUID identifier, UUID versionId) {
        var source = contentItemRepository.findById(versionId)
            .filter(version -> identifier.equals(version.getIdentifier()));
        if (source.isEmpty()) {
            log.info("Version `{}` not found for identifier `{}`; nothing to restore", versionId, identifier);
            return Optional.empty();
        }
        var src = source.get();
        var existingWorking = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);

        var toSave = workingVersionToSave(identifier, src.getContentType(), src.getSchemalessData(), existingWorking);
        var saved = contentItemRepository.save(toSave);
        log.info("Restored version `{}` into working version `{}` for identifier `{}`", versionId, saved.getVersionId(), identifier);
        return Optional.of(contentItemMapper.toResponse(saved));
    }

    /**
     * Returns the full editorial state (working + live, either may be null) for a content, or empty
     * (404) if the identifier is unknown.
     */
    public Optional<ContentItemWorkAndLiveDTO> getContentState(UUID identifier) {
        var versions = contentItemRepository.findByIdentifierOrderByVersionCreatedDatetimeDesc(identifier);
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        var working = firstInState(versions, ContentItemState.WORKING);
        var live = firstInState(versions, ContentItemState.LIVE);
        return Optional.of(new ContentItemWorkAndLiveDTO(working, live));
    }

    private ContentItemResponseDTO firstInState(List<ContentItemEntity> versions, ContentItemState state) {
        return versions.stream()
            .filter(version -> version.getState() == state)
            .findFirst()
            .map(contentItemMapper::toResponse)
            .orElse(null);
    }

    /**
     * Returns the version history (newest first) for a content, or empty (404) if the identifier is unknown.
     */
    public Optional<List<ContentItemVersionSummaryDTO>> getVersions(UUID identifier) {
        var versions = contentItemRepository.findByIdentifierOrderByVersionCreatedDatetimeDesc(identifier);
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versions.stream().map(contentItemMapper::toSummary).toList());
    }

    /**
     * Returns a single version's full content, or empty (404) if it is unknown or does not belong to
     * the identifier.
     */
    public Optional<ContentItemResponseDTO> getVersion(UUID identifier, UUID versionId) {
        return contentItemRepository.findById(versionId)
            .filter(version -> identifier.equals(version.getIdentifier()))
            .map(contentItemMapper::toResponse);
    }

    /**
     * Deletes every version of a logical content. The Elasticsearch document is removed after the
     * transaction commits (see {@code ContentItemIndexingListener}).
     */
    @Transactional
    public void deleteByIdentifier(UUID identifier) {
        log.info("Deleting all versions for identifier: {}", identifier);
        contentItemRepository.deleteByIdentifier(identifier);
        eventPublisher.publishEvent(new ContentItemDeletedEvent(identifier));
        log.info("Deleted contentItem identifier `{}`", identifier);
    }

    public List<ContentItemResponseDTO> findByIds(List<UUID> versionIds) {
        log.debug("Finding {} contentItem versions", versionIds.size());
        return contentItemRepository.findAllById(versionIds).stream()
            .map(contentItemMapper::toResponse)
            .toList();
    }

    // Builds the working version to persist: overwrites the existing working row's content in place
    // (an UPDATE) when one exists, otherwise mints a fresh working version to INSERT.
    private ContentItemEntity workingVersionToSave(UUID identifier, String contentType,
                                                   Map<String, Object> schemalessData,
                                                   Optional<ContentItemEntity> existingWorking) {
        return existingWorking
            .map(working -> working.withData(schemalessData))
            .orElseGet(() -> ContentItemEntity.newVersion(UuidV7.generate(), identifier, contentType,
                ContentItemState.WORKING, Instant.now(), schemalessData));
    }

    private void requireContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            throw new InvalidContentItemException("A contentItem must have a contentType.");
        }
    }
}
