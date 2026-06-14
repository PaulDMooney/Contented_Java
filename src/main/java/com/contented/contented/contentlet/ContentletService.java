package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import lombok.extern.log4j.Log4j2;
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

    public ResultPair save(ContentletEntity contentletEntity) {
        log.info("Saving contentlet: `{}`", contentletEntity.getId());
        var toSave = transformationHandler.applyTransformation(contentletEntity);
        if (toSave.getId() == null) {
            toSave.setId(UuidV7.generate());
            log.info("Assigned new id `{}` to contentlet", toSave.getId());
        }
        var resultPair = saveToDB(toSave);
        saveToES(resultPair.contentletEntity());
        return resultPair;
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

    private ResultPair saveToDB(ContentletEntity contentletEntity) {
        boolean exists = contentletRepository.existsById(contentletEntity.getId());
        boolean isNew = !exists;
        log.info("Contentlet {} already exists: {}", contentletEntity.getId(), exists);
        contentletEntity.setNew(isNew);
        var savedContentlet = contentletRepository.save(contentletEntity);
        log.info("Saved contentlet: `{}` successfully", savedContentlet.getId());
        return new ResultPair(savedContentlet, isNew);
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

    public record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
