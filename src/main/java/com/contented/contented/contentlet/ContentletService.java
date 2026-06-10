package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        var resultPair = saveToDB(toSave);
        saveToES(resultPair.contentletEntity());
        return resultPair;
    }

    private List<EntityAsMap> saveToES(ContentletEntity contentletEntity) {
        var indexedElasticSearchEntities = contentletIndexer.indexContentlet(contentletEntity).block();
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
        boolean exists = Boolean.TRUE.equals(contentletRepository.existsById(contentletEntity.getId()).block());
        boolean isNew = !exists;
        log.info("Contentlet {} already exists: {}", contentletEntity.getId(), exists);
        var savedContentlet = contentletRepository.save(contentletEntity).block();
        log.info("Saved contentlet: `{}` successfully", savedContentlet.getId());
        return new ResultPair(savedContentlet, isNew);
    }

    public void deleteById(String id) {
        log.info("Deleting contentlet: {}", id);
        deleteByIdFromDB(id);
        deleteByIdFromES(id);
        log.info("Deleted ES records for id: `{}` successfully", id);
    }

    private void deleteByIdFromES(String id) {
        contentletIndexer.deleteRecord(id).block();
    }

    private void deleteByIdFromDB(String id) {
        contentletRepository.deleteById(id).block();
        log.info("Deleted contentlet: `{}` successfully", id);
    }

    public Optional<ContentletEntity> findById(String id) {
        log.debug("Finding contentlet: {}", id);
        var result = Optional.ofNullable(contentletRepository.findById(id).block());
        if (result.isPresent()) {
            log.debug("Found contentlet: `{}` successfully", id);
        } else {
            log.debug("contentlet: `{}` not found", id);
        }
        return result;
    }

    public List<ContentletEntity> findByIds(List<String> ids) {
        log.debug("Finding {} contentlets", ids.size());
        return contentletRepository.findAllById(ids).collectList().block();
    }

    public record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
