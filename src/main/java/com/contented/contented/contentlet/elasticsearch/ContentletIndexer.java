package com.contented.contented.contentlet.elasticsearch;

import com.contented.contented.contentlet.ContentletEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ContentletIndexer {

    final ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    final IndexCoordinates indexCoordinates;

    @Autowired
    public ContentletIndexer(ReactiveElasticsearchOperations reactiveElasticsearchOperations, IndexCoordinates indexCoordinates) {
        this.reactiveElasticsearchOperations = reactiveElasticsearchOperations;
        this.indexCoordinates = indexCoordinates;
    }

    public Mono<ContentletEntity> indexContentlet(ContentletEntity contentletEntity) {
        return reactiveElasticsearchOperations.save(contentletEntity, indexCoordinates);
    }
}
