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
                    return contentletRepository.save(contentletEntity)
                            .map(savedContentlet -> new ResultPair(savedContentlet, isNew));
                });
    }

    record ResultPair(ContentletEntity contentletEntity, boolean isNew) {
    }
}
