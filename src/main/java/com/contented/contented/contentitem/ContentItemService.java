package com.contented.contented.contentitem;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.events.ContentItemDeletedEvent;
import com.contented.contented.contentitem.events.ContentItemPublishedEvent;
import com.contented.contented.contentitem.exceptions.ContentItemConflictException;
import com.contented.contented.contentitem.exceptions.InvalidContentItemException;
import com.contented.contented.contentitem.model.ContentItemVersionDTO;
import com.contented.contented.contentitem.model.ContentItemVersionEntity;
import com.contented.contented.contentitem.model.ContentItemMapper;
import com.contented.contented.contentitem.model.ContentItemVersionResponseDTO;
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
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class ContentItemService {

    private final ContentItemVersionRepository contentItemRepository;

    private final TransformationHandler transformationHandler;

    private final ContentItemMapper contentItemMapper;

    private final ApplicationEventPublisher eventPublisher;

    public ContentItemService(ContentItemVersionRepository contentItemRepository, TransformationHandler transformationHandler,
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
    public ContentItemVersionResponseDTO create(ContentItemVersionDTO dto) {
        requireContentType(dto.getContentType());
        var transformed = transformationHandler.applyTransformation(contentItemMapper.toEntity(dto));
        var toSave = transformed.asNewVersion(UuidV7.generate(), UuidV7.generate(), ContentItemState.WORKING, Instant.now());
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
    public Optional<ContentItemVersionResponseDTO> updateWorking(UUID identifier, ContentItemVersionDTO dto) {
        requireContentType(dto.getContentType());
        var existingWorking = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);
        // The stored contentType (and the content's existence) can be read from the working version if
        // present, otherwise the live one; a known content always has one or the other (no operation
        // leaves only archived versions), so their absence means the identifier is unknown (empty -> 404).
        return existingWorking
            .or(() -> contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.LIVE))
            .map(reference -> {
                // contentType is fixed across all versions of a content.
                var storedContentType = reference.getContentType();
                if (!storedContentType.equalsIgnoreCase(dto.getContentType())) {
                    throw new InvalidContentItemException(
                        "A contentItem's contentType cannot be changed; `" + storedContentType
                            + "` was created, `" + dto.getContentType() + "` was supplied.");
                }
                var transformed = transformationHandler.applyTransformation(contentItemMapper.toEntity(dto));
                // Preserve the stored contentType so its casing never drifts on edit.
                var saved = contentItemRepository.save(
                    workingVersionToSave(identifier, storedContentType, transformed, existingWorking));
                log.info("Saved working contentItem version `{}` for identifier `{}`", saved.getVersionId(), identifier);
                return contentItemMapper.toResponse(saved);
            });
    }

    /**
     * Publishes the WORKING draft: the current LIVE version (if any) is demoted to ARCHIVED, then the
     * working version is promoted to LIVE in place. Returns empty for an unknown identifier (404);
     * throws {@link ContentItemConflictException} (409) when the content exists but has nothing to
     * publish. The now-live version is indexed after the transaction commits (see
     * {@code ContentItemIndexingListener}).
     */
    @Transactional
    public Optional<ContentItemVersionResponseDTO> publish(UUID identifier) {
        var working = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);
        if (working.isEmpty()) {
            if (!contentItemRepository.existsByIdentifier(identifier)) {
                log.info("No content for identifier `{}`; nothing to publish", identifier);
                return Optional.empty();
            }
            throw new ContentItemConflictException(
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
     * Restores a previous version into the WORKING draft of its own content by copying its content
     * (history is never modified). The restored content can then be published. Returns empty (404)
     * if the version is unknown.
     */
    @Transactional
    public Optional<ContentItemVersionResponseDTO> restore(UUID versionId) {
        return contentItemRepository.findById(versionId)
            .map(source -> {
                var identifier = source.getIdentifier();
                var existingWorking = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);
                var saved = contentItemRepository.save(
                    workingVersionToSave(identifier, source.getContentType(), source, existingWorking));
                log.info("Restored version `{}` into working version `{}` for identifier `{}`",
                    versionId, saved.getVersionId(), identifier);
                return contentItemMapper.toResponse(saved);
            });
    }

    /**
     * Returns the full editorial state (working + live, either may be null) for a content, or empty
     * (404) if the identifier is unknown.
     */
    public Optional<ContentItemWorkAndLiveDTO> getContentState(UUID identifier) {
        var working = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.WORKING);
        var live = contentItemRepository.findByIdentifierAndState(identifier, ContentItemState.LIVE);
        // A known content always has a WORKING and/or LIVE version (no operation leaves only archived
        // versions); their mutual absence means the identifier is unknown.
        if (working.isEmpty() && live.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ContentItemWorkAndLiveDTO(
            working.map(contentItemMapper::toResponse).orElse(null),
            live.map(contentItemMapper::toResponse).orElse(null)));
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
     * Returns a single version's full content, or empty (404) if it is unknown.
     */
    public Optional<ContentItemVersionResponseDTO> getVersion(UUID versionId) {
        return contentItemRepository.findById(versionId)
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

    public List<ContentItemVersionResponseDTO> findByIds(List<UUID> versionIds) {
        log.debug("Finding {} contentItem versions", versionIds.size());
        return contentItemRepository.findAllById(versionIds).stream()
            .map(contentItemMapper::toResponse)
            .toList();
    }

    // Builds the working version to persist: overwrites the existing working row's content in place
    // (an UPDATE) when one exists, otherwise promotes the draft into a fresh working version to
    // INSERT. `draft` carries the content to use (the transformed entity for an edit, the source
    // version for a restore); `contentType` is passed explicitly so a lazily-created working version
    // always matches the content's already-established casing rather than the draft's own.
    private ContentItemVersionEntity workingVersionToSave(UUID identifier, String contentType,
                                                   ContentItemVersionEntity draft,
                                                   Optional<ContentItemVersionEntity> existingWorking) {
        return existingWorking
            .map(working -> working.withData(draft.getData()))
            .orElseGet(() -> draft.asNewVersion(UuidV7.generate(), identifier, contentType,
                ContentItemState.WORKING, Instant.now()));
    }

    private void requireContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            throw new InvalidContentItemException("A contentItem must have a contentType.");
        }
    }
}
