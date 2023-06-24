package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.swing.text.AbstractDocument;
import java.util.List;

@Log4j2
@Service
public class ContentletService {

    private final ContentletRepository contentletRepository;

    private final ContentletIndexer contentletIndexer;

    public ContentletService(ContentletRepository contentletRepository, ContentletIndexer contentletIndexer) {
        this.contentletRepository = contentletRepository;
        this.contentletIndexer = contentletIndexer;
    }

    public Mono<ResultPair> save(ContentletEntity contentletEntity) {
        log.info("Saving contentlet: {}", contentletEntity.getId());
        return saveToDB(contentletEntity)
            .flatMap(resultPair ->  contentletIndexer.indexContentlet(resultPair.contentletEntity())
                .doOnSuccess(indexedContentlet ->
                    log.info("Indexed contentlet: {} successfully", indexedContentlet.getId()))
                .map(indexedContentlet -> resultPair));
    }

    private Mono<ResultPair> saveToDB(ContentletEntity contentletEntity) {
        return contentletRepository.existsById(contentletEntity.getId())
            .flatMap(exists -> {
                boolean isNew = !exists;
                log.info("Contentlet {} already exists: {}", contentletEntity.getId(), exists);
                return contentletRepository.save(contentletEntity)
                    .map(savedContentlet -> new ResultPair(savedContentlet, isNew))
                    .doOnSuccess(resultPair -> log.info("Saved contentlet: {} successfully", resultPair.contentletEntity().getId()));
            });
    }

    public Mono<Void> deleteById(String id) {
        log.info("Deleting contentlet: {}", id);
        return contentletRepository.deleteById(id)
                .doOnSuccess(result -> log.info("Deleted contentlet: {} successfully", id));
    }

    public Mono<ContentletEntity> findById(String id) {
        log.debug("Finding contentlet: {}", id);
        return contentletRepository.findById(id)
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.debug("Found contentlet: {} successfully", id);
                    } else {
                        log.debug("contentlet: {} not found", id);
                    }
                });
    }

    public Flux<ContentletEntity> findByIds(List<String> ids) {
        log.debug("Finding {} contentlets", ids.size());
        return contentletRepository.findAllById(ids);
    }

    record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
