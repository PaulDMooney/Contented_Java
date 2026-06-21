package com.contented.contented.contentitem;

import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class ContentItemService {

    private final ContentItemRepository contentItemRepository;

    private final ContentItemIndexer contentItemIndexer;

    private final TransformationHandler transformationHandler;

    private final ContentItemMapper contentItemMapper;

    public ContentItemService(ContentItemRepository contentItemRepository, ContentItemIndexer contentItemIndexer, TransformationHandler transformationHandler, ContentItemMapper contentItemMapper) {
        this.contentItemRepository = contentItemRepository;
        this.contentItemIndexer = contentItemIndexer;
        this.transformationHandler = transformationHandler;
        this.contentItemMapper = contentItemMapper;
    }

    public ContentItemResponseDTO create(ContentItemDTO dto) {
        requireContentType(dto.getContentType());
        var toSave = transformationHandler.applyTransformation(contentItemMapper.toEntity(dto));
        toSave.setId(UuidV7.generate());
        toSave.setNew(true);
        var saved = contentItemRepository.save(toSave);
        log.info("Created contentItem: `{}` successfully", saved.getId());
        saveToES(saved);
        return contentItemMapper.toResponse(saved);
    }

    public Optional<ContentItemResponseDTO> update(UUID id, ContentItemDTO dto) {
        requireContentType(dto.getContentType());
        var existing = contentItemRepository.findById(id);
        if (existing.isEmpty()) {
            log.info("ContentItem `{}` not found; nothing to update", id);
            return Optional.empty();
        }
        // A contentItem's contentType is fixed at creation.
        if (!existing.get().getContentType().equalsIgnoreCase(dto.getContentType())) {
            throw new InvalidContentItemException(
                "A contentItem's contentType cannot be changed; `" + existing.get().getContentType()
                    + "` was created, `" + dto.getContentType() + "` was supplied.");
        }
        var toSave = transformationHandler.applyTransformation(contentItemMapper.toEntity(dto));
        toSave.setId(id);
        // Preserve the stored contentType so its casing never drifts on update.
        toSave.setContentType(existing.get().getContentType());
        toSave.setNew(false);
        var saved = contentItemRepository.save(toSave);
        log.info("Updated contentItem: `{}` successfully", saved.getId());
        saveToES(saved);
        return Optional.of(contentItemMapper.toResponse(saved));
    }

    private void requireContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            throw new InvalidContentItemException("A contentItem must have a contentType.");
        }
    }

    private List<EntityAsMap> saveToES(ContentItemEntity contentItemEntity) {
        var indexedElasticSearchEntities = contentItemIndexer.indexContentItem(contentItemEntity);
        if (indexedElasticSearchEntities == null) {
            return Collections.emptyList();
        }
        log.info("Indexed `{}` documents for contentItem: `{}` successfully",
            indexedElasticSearchEntities.size(),
            contentItemEntity.getId()
        );
        return indexedElasticSearchEntities;
    }

    public void deleteById(UUID id) {
        log.info("Deleting contentItem: {}", id);
        deleteByIdFromDB(id);
        deleteByIdFromES(id);
        log.info("Deleted ES records for id: `{}` successfully", id);
    }

    private void deleteByIdFromES(UUID id) {
        contentItemIndexer.deleteRecord(id.toString());
    }

    private void deleteByIdFromDB(UUID id) {
        contentItemRepository.deleteById(id);
        log.info("Deleted contentItem: `{}` successfully", id);
    }

    public List<ContentItemResponseDTO> findAll() {
        return contentItemRepository.findAll().stream()
            .map(contentItemMapper::toResponse)
            .toList();
    }

    public Optional<ContentItemResponseDTO> findById(UUID id) {
        log.debug("Finding contentItem: {}", id);
        var result = contentItemRepository.findById(id);
        if (result.isPresent()) {
            log.debug("Found contentItem: `{}` successfully", id);
        } else {
            log.debug("contentItem: `{}` not found", id);
        }
        return result.map(contentItemMapper::toResponse);
    }

    public List<ContentItemResponseDTO> findByIds(List<UUID> ids) {
        log.debug("Finding {} contentItems", ids.size());
        return contentItemRepository.findAllById(ids).stream()
            .map(contentItemMapper::toResponse)
            .toList();
    }
}
