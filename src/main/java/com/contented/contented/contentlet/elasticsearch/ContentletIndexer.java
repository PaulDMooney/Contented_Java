package com.contented.contented.contentlet.elasticsearch;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.elasticsearch.transformation.ESRecordTransformer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Log4j2
@Component
public class ContentletIndexer {

    final ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    final IndexCoordinates indexCoordinates;

    final List<ESRecordTransformer> esTransformers;

    @Autowired
    public ContentletIndexer(ReactiveElasticsearchOperations reactiveElasticsearchOperations,
                             IndexCoordinates indexCoordinates,
                             List<ESRecordTransformer> esTransformers) {
        this.reactiveElasticsearchOperations = reactiveElasticsearchOperations;
        this.indexCoordinates = indexCoordinates;
        this.esTransformers = esTransformers;
    }

    public Mono<EntityAsMap> indexContentlet(ContentletEntity contentletEntity) {
        return esTransformers.stream()
                .filter(esRecordTransformer -> esRecordTransformer.test(contentletEntity))
                .findFirst()
                .map(esRecordTransformer -> {
                    var transformedEntity = esRecordTransformer.transform(contentletEntity);
                    return reactiveElasticsearchOperations.save(transformedEntity, indexCoordinates);
                })
                .orElseGet(() -> {
                    log.warn("No transformer found for contentlet: {}", contentletEntity.getId());
                    return Mono.empty();
                });
    }

    public Mono<String> deleteRecord(String id) {
        return reactiveElasticsearchOperations.delete(id, indexCoordinates);
    }
}
