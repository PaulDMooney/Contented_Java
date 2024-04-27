package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public Mono<ContentletSaveResult> save(ContentletEntity contentletEntity) {
        log.info("Saving contentlet: `{}`", contentletEntity.getId());
        var toSave = transformationHandler.applyTransformation(contentletEntity);
        return saveToDB(toSave)
            .flatMap(contentletSaveResult -> saveToES(contentletSaveResult.contentletEntity(), contentletSaveResult.previousContentletEntityVersion())
                    .defaultIfEmpty(Collections.EMPTY_LIST) // Is there a better way to handle a potentially empty mono here?
                .map(indexedElasticSearchEntities -> contentletSaveResult)
            );
    }

    private Mono<List<EntityAsMap>> saveToES(ContentletEntity contentletEntity, ContentletEntity previousContentletEntityVersion) {
        return contentletIndexer.indexContentlet(contentletEntity, previousContentletEntityVersion)
            .doOnNext(elasticSearchEntities ->
                    log.info("Indexed `{}` documents for contentlet: `{}` successfully",
                        elasticSearchEntities.size(),
                        contentletEntity.getId()
                    )
            );
    }

    private Mono<ContentletSaveResult> saveToDB(ContentletEntity contentletEntity) {
        return contentletRepository.findById(contentletEntity.getId())
            .map(oldContentlet -> Optional.of(oldContentlet))
            .switchIfEmpty(Mono.just(Optional.empty()))
            .flatMap(oldContentlet -> {
                boolean isNew = oldContentlet.isEmpty();
                log.info("Contentlet {} already exists: {}", contentletEntity.getId(), !isNew);
                return contentletRepository.save(contentletEntity)
                    .map(savedContentlet -> new ContentletSaveResult(savedContentlet, isNew, oldContentlet.orElse(null)))
                    .doOnNext(contentletSaveResult -> log.info("Saved contentlet: `{}` successfully", contentletSaveResult.contentletEntity().getId()));
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

    public record ContentletSaveResult(ContentletEntity contentletEntity,
                                       boolean isNew,
                                       @Nullable ContentletEntity previousContentletEntityVersion) {
    }
}
