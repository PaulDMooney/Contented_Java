package com.contented.contented.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@DisplayName("ElasticSearchIndexCreator")
class ElasticSearchIndexCreatorTest {

    static final String INDEX_NAME = "testindex";
    static final String MAPPINGS_FILE = "elasticsearch/mappings.json";

    static ElasticsearchIndicesClient mockIndicesClient(ElasticsearchClient elasticsearchClient) {
        var indicesClient = Mockito.mock(ElasticsearchIndicesClient.class);
        Mockito.when(elasticsearchClient.indices()).thenReturn(indicesClient);
        return indicesClient;
    }

    static ElasticsearchException elasticsearchExceptionWithType(String errorType) {
        ErrorResponse errorResponse = ErrorResponse.of(b -> b
            .status(400)
            .error(e -> e.type(errorType).reason("simulated error for test")));
        return new ElasticsearchException("indices.create", errorResponse);
    }

    @Nested
    @DisplayName("createIndex()")
    class CreateIndex {

        @Nested
        @DisplayName("When Elasticsearch acknowledges the index creation")
        class WhenAcknowledged {

            ArgumentCaptor<CreateIndexRequest> requestCaptor = ArgumentCaptor.forClass(CreateIndexRequest.class);
            IndexCreationResult result;

            @BeforeAll
            void when() throws IOException {
                var elasticsearchClient = Mockito.mock(ElasticsearchClient.class);
                var indicesClient = mockIndicesClient(elasticsearchClient);
                Mockito.when(indicesClient.create(any(CreateIndexRequest.class)))
                    .thenReturn(CreateIndexResponse.of(b -> b.acknowledged(true).shardsAcknowledged(true).index(INDEX_NAME)));

                var indexCreator = new ElasticSearchIndexCreator(elasticsearchClient, IndexCoordinates.of(INDEX_NAME), MAPPINGS_FILE);
                result = indexCreator.createIndex();

                verify(indicesClient).create(requestCaptor.capture());
            }

            @Test
            @DisplayName("It should return `CREATED`")
            void shouldReturnCreated() {
                assertThat(result).isEqualTo(IndexCreationResult.CREATED);
            }

            @Test
            @DisplayName("It should request creation of the configured index name")
            void shouldRequestConfiguredIndexName() {
                assertThat(requestCaptor.getValue().index()).isEqualTo(INDEX_NAME);
            }
        }

        @Nested
        @DisplayName("When Elasticsearch does not acknowledge the index creation")
        class WhenNotAcknowledged {

            IndexCreationResult result;

            @BeforeAll
            void when() throws IOException {
                var elasticsearchClient = Mockito.mock(ElasticsearchClient.class);
                var indicesClient = mockIndicesClient(elasticsearchClient);
                Mockito.when(indicesClient.create(any(CreateIndexRequest.class)))
                    .thenReturn(CreateIndexResponse.of(b -> b.acknowledged(false).shardsAcknowledged(false).index(INDEX_NAME)));

                var indexCreator = new ElasticSearchIndexCreator(elasticsearchClient, IndexCoordinates.of(INDEX_NAME), MAPPINGS_FILE);
                result = indexCreator.createIndex();
            }

            @Test
            @DisplayName("It should return `FAILED`")
            void shouldReturnFailed() {
                assertThat(result).isEqualTo(IndexCreationResult.FAILED);
            }
        }

        @Nested
        @DisplayName("When Elasticsearch rejects the request because the index already exists")
        class WhenIndexAlreadyExists {

            IndexCreationResult result;

            @BeforeAll
            void when() throws IOException {
                var elasticsearchClient = Mockito.mock(ElasticsearchClient.class);
                var indicesClient = mockIndicesClient(elasticsearchClient);
                Mockito.when(indicesClient.create(any(CreateIndexRequest.class)))
                    .thenThrow(elasticsearchExceptionWithType("resource_already_exists_exception"));

                var indexCreator = new ElasticSearchIndexCreator(elasticsearchClient, IndexCoordinates.of(INDEX_NAME), MAPPINGS_FILE);
                result = indexCreator.createIndex();
            }

            @Test
            @DisplayName("It should return `ALREADY_EXISTS` instead of throwing")
            void shouldReturnAlreadyExists() {
                assertThat(result).isEqualTo(IndexCreationResult.ALREADY_EXISTS);
            }
        }

        @Nested
        @DisplayName("When Elasticsearch rejects the request for a reason other than the index already existing")
        class WhenRejectedForOtherReason {

            @Test
            @DisplayName("It should rethrow the `ElasticsearchException`")
            void shouldRethrowException() throws IOException {
                var elasticsearchClient = Mockito.mock(ElasticsearchClient.class);
                var indicesClient = mockIndicesClient(elasticsearchClient);
                var thrown = elasticsearchExceptionWithType("mapper_parsing_exception");
                Mockito.when(indicesClient.create(any(CreateIndexRequest.class))).thenThrow(thrown);

                var indexCreator = new ElasticSearchIndexCreator(elasticsearchClient, IndexCoordinates.of(INDEX_NAME), MAPPINGS_FILE);

                assertThatThrownBy(indexCreator::createIndex).isSameAs(thrown);
            }
        }
    }
}
