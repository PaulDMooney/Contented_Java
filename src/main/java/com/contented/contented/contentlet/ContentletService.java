package com.contented.contented.contentlet;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class ContentletService {

    private final ContentletRepository contentletRepository;

    public ContentletService(ContentletRepository contentletRepository) {
        this.contentletRepository = contentletRepository;
    }

    public Mono<ResultPair> save(ContentletEntity contentletEntity) {
        log.info("Saving contentlet: {}", contentletEntity.getId());
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

    record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
