package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.contented.contented.contentlet.elasticsearch.ElasticSearchConfig.INDEX_PROPERTY_KEY;

@Component
@Log4j2
public class ElasticSearchIndexCreator {

    public static final String MAPPINGS_FILE_PROPERTY_KEY = "elasticsearch.index.mappingsfile";

    final ReactiveElasticsearchClient reactiveElasticsearchClient;

    final String indexName;

    final String mappingsFile;

    @Autowired
    public ElasticSearchIndexCreator(ReactiveElasticsearchClient reactiveElasticsearchClient,
                                     @Value("${"+ INDEX_PROPERTY_KEY +"}") String indexName,
                                     @Value("${"+MAPPINGS_FILE_PROPERTY_KEY+"}") String mappingsFile) {
        this.reactiveElasticsearchClient = reactiveElasticsearchClient;
        this.indexName = indexName;
        this.mappingsFile = mappingsFile;
    }

    public Mono<Boolean> createIndex() {
        var createIndexRequest = CreateIndexRequest.of(builder ->
            builder.index(indexName)
                .mappings(mappingsBuilder -> {
                var mappingJson = this.getClass().getClassLoader().getResourceAsStream(mappingsFile);
                return mappingsBuilder.withJson(mappingJson);
            }));

        log.info("Creating index {}", indexName);
        return reactiveElasticsearchClient.indices().create(createIndexRequest)
            .map(response -> {
                if (response.acknowledged()) {
                    log.info("Index {} created", indexName);
                    return true;
                } else {
                    log.error("Index {} not created", indexName);
                    return false;
                }
            });

        // TODO: Throw error if response is not successful
    }
}
