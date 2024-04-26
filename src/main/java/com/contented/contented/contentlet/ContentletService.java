package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public Mono<ResultPair> save(ContentletEntity contentletEntity) {
        log.info("Saving contentlet: `{}`", contentletEntity.getId());
        var toSave = transformationHandler.applyTransformation(contentletEntity);
        return saveToDB(toSave)
            .flatMap(resultPair -> saveToES(resultPair.contentletEntity())
                    .defaultIfEmpty(Collections.EMPTY_LIST) // Is there a better way to handle a potentially empty mono here?
                .map(indexedElasticSearchEntities -> resultPair)
            );
    }

    private Mono<List<EntityAsMap>> saveToES(ContentletEntity contentletEntity) {
        return contentletIndexer.indexContentlet(contentletEntity)
            .doOnNext(elasticSearchEntities ->
                    log.info("Indexed `{}` documents for contentlet: `{}` successfully",
                        elasticSearchEntities.size(),
                        contentletEntity.getId()
                    )
            );
    }

    private Mono<ResultPair> saveToDB(ContentletEntity contentletEntity) {
        return contentletRepository.existsById(contentletEntity.getId())
            .flatMap(exists -> {
                boolean isNew = !exists;
                log.info("Contentlet {} already exists: {}", contentletEntity.getId(), exists);
                return contentletRepository.save(contentletEntity)
                    .map(savedContentlet -> new ResultPair(savedContentlet, isNew))
                    .doOnNext(resultPair -> log.info("Saved contentlet: `{}` successfully", resultPair.contentletEntity().getId()));
            });
    }

    public Mono<String> deleteById(String id) {
        log.info("Deleting contentlet: {}", id);
        return deleteByIdFromDB(id)
                .then(deleteByIdFromES(id))
                .doOnSuccess(result -> log.info("Deleted ES records for id: `{}` successfully", id));
    }

    private Mono<String> deleteByIdFromES(String id) {
        return contentletIndexer.deleteRecord(id);
    }

    private Mono<Void> deleteByIdFromDB(String id) {
        return contentletRepository.deleteById(id)
                .doOnSuccess(result -> log.info("Deleted contentlet: `{}` successfully", id));
    }

    public Mono<ContentletEntity> findById(String id) {
        log.debug("Finding contentlet: {}", id);
        return contentletRepository.findById(id)
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.debug("Found contentlet: `{}` successfully", id);
                    } else {
                        log.debug("contentlet: `{}` not found", id);
                    }
                });
    }

    public Flux<ContentletEntity> findByIds(List<String> ids) {
        log.debug("Finding {} contentlets", ids.size());
        return contentletRepository.findAllById(ids);
    }

    public record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
