package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        ResultPair resultPair = saveToDB(toSave);
        saveToES(resultPair.contentletEntity());
        return resultPair;
    }

    private List<EntityAsMap> saveToES(ContentletEntity contentletEntity) {
        List<EntityAsMap> elasticSearchEntities = contentletIndexer.indexContentlet(contentletEntity);
        log.info("Indexed `{}` documents for contentlet: `{}` successfully",
                elasticSearchEntities.size(),
                contentletEntity.getId()
        );
        return elasticSearchEntities;
    }

    private ResultPair saveToDB(ContentletEntity contentletEntity) {
        boolean exists = contentletRepository.existsById(contentletEntity.getId());
        boolean isNew = !exists;
        log.info("Contentlet {} already exists: {}", contentletEntity.getId(), exists);
        ContentletEntity savedContentlet = contentletRepository.save(contentletEntity);
        log.info("Saved contentlet: `{}` successfully", savedContentlet.getId());
        return new ResultPair(savedContentlet, isNew);
    }

    public String deleteById(String id) {
        log.info("Deleting contentlet: {}", id);
        deleteByIdFromDB(id);
        deleteByIdFromES(id);
        log.info("Deleted ES records for id: `{}` successfully", id);
        return id;
    }

    private String deleteByIdFromES(String id) {
        return contentletIndexer.deleteRecord(id);
    }

    private void deleteByIdFromDB(String id) {
        contentletRepository.deleteById(id);
        log.info("Deleted contentlet: `{}` successfully", id);
    }

    public ContentletEntity findById(String id) {
        log.debug("Finding contentlet: {}", id);
        ContentletEntity result = contentletRepository.findById(id);
        if (result != null) {
            log.debug("Found contentlet: `{}` successfully", id);
        } else {
            log.debug("contentlet: `{}` not found", id);
        }
        return result;
    }

    public List<ContentletEntity> findByIds(List<String> ids) {
        log.debug("Finding {} contentlets", ids.size());
        return contentletRepository.findAllById(ids);
    }

    public record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
