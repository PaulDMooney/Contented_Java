package com.contented.contented.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
@Log4j2
public class ElasticSearchIndexCreator {

    public static final String MAPPINGS_FILE_PROPERTY_KEY = "elasticsearch.index.mappingsfile";

    private static final String RESOURCE_ALREADY_EXISTS_ERROR_TYPE = "resource_already_exists_exception";

    final ElasticsearchClient elasticsearchClient;

    final IndexCoordinates indexCoordinates;

    final String mappingsFile;

    @Autowired
    public ElasticSearchIndexCreator(ElasticsearchClient elasticsearchClient,
                                     IndexCoordinates indexCoordinates,
                                     @Value("${"+MAPPINGS_FILE_PROPERTY_KEY+"}") String mappingsFile) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexCoordinates = indexCoordinates;
        this.mappingsFile = mappingsFile;
    }

    public IndexCreationResult createIndex() {
        var createIndexRequest = CreateIndexRequest.of(builder ->
            builder.index(indexCoordinates.getIndexName())
                .mappings(mappingsBuilder -> {
                var mappingJson = this.getClass().getClassLoader().getResourceAsStream(mappingsFile);
                return mappingsBuilder.withJson(mappingJson);
            }));

        log.info("Creating index {}", indexCoordinates.getIndexName());
        try {
            var response = elasticsearchClient.indices().create(createIndexRequest);
            if (response.acknowledged()) {
                log.info("Index {} created", indexCoordinates.getIndexName());
                return IndexCreationResult.CREATED;
            } else {
                log.error("Index {} not created", indexCoordinates.getIndexName());
                return IndexCreationResult.FAILED;
            }
        } catch (ElasticsearchException exception) {
            if (RESOURCE_ALREADY_EXISTS_ERROR_TYPE.equals(exception.error().type())) {
                log.info("Index {} already exists", indexCoordinates.getIndexName());
                return IndexCreationResult.ALREADY_EXISTS;
            }
            throw exception;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
