package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Log4j2
public class ElasticSearchIndexCreator {

    public static final String MAPPINGS_FILE_PROPERTY_KEY = "elasticsearch.index.mappingsfile";

    final ReactiveElasticsearchClient reactiveElasticsearchClient;

    final IndexCoordinates indexCoordinates;

    final String mappingsFile;

    @Autowired
    public ElasticSearchIndexCreator(ReactiveElasticsearchClient reactiveElasticsearchClient,
                                     IndexCoordinates indexCoordinates,
                                     @Value("${"+MAPPINGS_FILE_PROPERTY_KEY+"}") String mappingsFile) {
        this.reactiveElasticsearchClient = reactiveElasticsearchClient;
        this.indexCoordinates = indexCoordinates;
        this.mappingsFile = mappingsFile;
    }

    public Mono<Boolean> createIndex() {
        var createIndexRequest = CreateIndexRequest.of(builder ->
            builder.index(indexCoordinates.getIndexName())
                .mappings(mappingsBuilder -> {
                var mappingJson = this.getClass().getClassLoader().getResourceAsStream(mappingsFile);
                return mappingsBuilder.withJson(mappingJson);
            }));

        log.info("Creating index {}", indexCoordinates.getIndexName());
        return reactiveElasticsearchClient.indices().create(createIndexRequest)
            .map(response -> {
                if (response.acknowledged()) {
                    log.info("Index {} created", indexCoordinates.getIndexName());
                    return true;
                } else {
                    log.error("Index {} not created", indexCoordinates.getIndexName());
                    return false;
                }
            });

        // TODO: Throw error if response is not successful
    }
}
