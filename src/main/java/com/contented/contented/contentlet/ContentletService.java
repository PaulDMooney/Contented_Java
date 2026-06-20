package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
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
public class ContentletService {

    private final ContentletRepository contentletRepository;

    private final ContentletIndexer contentletIndexer;

    private final TransformationHandler transformationHandler;

    public ContentletService(ContentletRepository contentletRepository, ContentletIndexer contentletIndexer, TransformationHandler transformationHandler) {
        this.contentletRepository = contentletRepository;
        this.contentletIndexer = contentletIndexer;
        this.transformationHandler = transformationHandler;
    }

    public ContentletEntity create(CreateContentletCommand command) {
        requireContentType(command.contentType());
        var toSave = transformationHandler.applyTransformation(
            new ContentletEntity(null, command.contentType(), command.data()));
        toSave.setId(UuidV7.generate());
        toSave.setNew(true);
        var saved = contentletRepository.save(toSave);
        log.info("Created contentlet: `{}` successfully", saved.getId());
        saveToES(saved);
        return saved;
    }

    public Optional<ContentletEntity> update(UpdateContentletCommand command) {
        requireContentType(command.contentType());
        var existing = contentletRepository.findById(command.id());
        if (existing.isEmpty()) {
            log.info("Contentlet `{}` not found; nothing to update", command.id());
            return Optional.empty();
        }
        // A contentlet's contentType is fixed at creation.
        if (!existing.get().getContentType().equalsIgnoreCase(command.contentType())) {
            throw new InvalidContentletException(
                "A contentlet's contentType cannot be changed; `" + existing.get().getContentType()
                    + "` was created, `" + command.contentType() + "` was supplied.");
        }
        var toSave = transformationHandler.applyTransformation(
            new ContentletEntity(command.id(), command.contentType(), command.data()));
        toSave.setId(command.id());
        // Preserve the stored contentType so its casing never drifts on update.
        toSave.setContentType(existing.get().getContentType());
        toSave.setNew(false);
        var saved = contentletRepository.save(toSave);
        log.info("Updated contentlet: `{}` successfully", saved.getId());
        saveToES(saved);
        return Optional.of(saved);
    }

    private void requireContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            throw new InvalidContentletException("A contentlet must have a contentType.");
        }
    }

    private List<EntityAsMap> saveToES(ContentletEntity contentletEntity) {
        var indexedElasticSearchEntities = contentletIndexer.indexContentlet(contentletEntity);
        if (indexedElasticSearchEntities == null) {
            return Collections.emptyList();
        }
        log.info("Indexed `{}` documents for contentlet: `{}` successfully",
            indexedElasticSearchEntities.size(),
            contentletEntity.getId()
        );
        return indexedElasticSearchEntities;
    }

    public void deleteById(UUID id) {
        log.info("Deleting contentlet: {}", id);
        deleteByIdFromDB(id);
        deleteByIdFromES(id);
        log.info("Deleted ES records for id: `{}` successfully", id);
    }

    private void deleteByIdFromES(UUID id) {
        contentletIndexer.deleteRecord(id.toString());
    }

    private void deleteByIdFromDB(UUID id) {
        contentletRepository.deleteById(id);
        log.info("Deleted contentlet: `{}` successfully", id);
    }

    public Optional<ContentletEntity> findById(UUID id) {
        log.debug("Finding contentlet: {}", id);
        var result = contentletRepository.findById(id);
        if (result.isPresent()) {
            log.debug("Found contentlet: `{}` successfully", id);
        } else {
            log.debug("contentlet: `{}` not found", id);
        }
        return result;
    }

    public List<ContentletEntity> findByIds(List<UUID> ids) {
        log.debug("Finding {} contentlets", ids.size());
        return contentletRepository.findAllById(ids);
    }
}
