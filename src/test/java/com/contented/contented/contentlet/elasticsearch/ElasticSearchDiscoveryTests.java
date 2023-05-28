package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ElasticSearch discovery tests")
public class ElasticSearchDiscoveryTests {

//    @Container
//    static MongoDBContainer mongoDBContainer = mongoDBContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @Autowired
    ReactiveElasticsearchClient reactiveElasticsearchClient;

    @Autowired
    ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
//        startAndRegsiterMongoDBContainer(mongoDBContainer, registry);
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
    }

    // New instance is initialized in parent container above. May cause issues later due to test execution order??
    @NestedPerClass
    @DisplayName("Given a new instance of elastic search")
    class GivenANewInstance {

        static final String INDEX_NAME = "mytestindex2";

        @Test
        @DisplayName("No index should exist yet")
        void no_index_should_exist_yet() {

            var indexExistsRequest = ExistsRequest.of(builder ->
                builder.index(INDEX_NAME)
            );

            var result = reactiveElasticsearchClient.indices().exists(indexExistsRequest)
                .block();

            Assertions.assertThat(result.value()).isFalse();
        }

        @NestedPerClass
        @DisplayName("When an index is created")
        class WhenAnIndexIsCreated {

            CreateIndexResponse response;

            @BeforeAll
            void when() {
                var createIndexRequest = CreateIndexRequest.of(builder ->
                    builder.index(INDEX_NAME)
                );

                response = reactiveElasticsearchClient.indices().create(createIndexRequest)
                    .block();
            }

            @Test
            @DisplayName("The response should be acknowledged and return the index name")
            void the_response_should_be_acknowledged() {

                Assertions.assertThat(response.acknowledged()).isTrue();
                Assertions.assertThat(response.index()).isEqualTo(INDEX_NAME);
            }

            @Test
            @DisplayName("The index should exist")
            void the_index_should_exist() {

                var indexExistsRequest = ExistsRequest.of(builder ->
                    builder.index(INDEX_NAME)
                );

                var result = reactiveElasticsearchClient.indices().exists(indexExistsRequest)
                    .block();

                Assertions.assertThat(result.value()).isTrue();
            }
        }

        @NestedPerClass
        @DisplayName("When an index with mappings is created")
        class WhenAnIndexWithSettingsIsCreated {

            CreateIndexResponse response;

            @BeforeAll
            void when() {
                var createIndexRequest = CreateIndexRequest.of(builder ->

                    builder.index(INDEX_NAME).mappings(mappingsBuilder -> {
                            var mappingJson = this.getClass().getResourceAsStream("/elasticsearch/mappings.json");
                            return mappingsBuilder.withJson(mappingJson);
                        }
                    )
                );

                response = reactiveElasticsearchClient.indices().create(createIndexRequest)
                    .block();
            }

            @Test
            @DisplayName("The response should be acknowledged and return the index name")
            void the_response_should_be_acknowledged() {

                Assertions.assertThat(response.acknowledged()).isTrue();
                Assertions.assertThat(response.index()).isEqualTo(INDEX_NAME);
            }

            @Test
            @DisplayName("The index should exist")
            void the_index_should_exist() {

                var indexExistsRequest = ExistsRequest.of(builder ->
                    builder.index(INDEX_NAME)
                );

                var result = reactiveElasticsearchClient.indices().exists(indexExistsRequest)
                    .block();

                Assertions.assertThat(result.value()).isTrue();
            }
        }
    }


}
