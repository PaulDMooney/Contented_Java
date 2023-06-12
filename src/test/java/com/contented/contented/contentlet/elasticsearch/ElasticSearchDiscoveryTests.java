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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers(parallel = true)
@DisplayName("ElasticSearch discovery tests")
public class ElasticSearchDiscoveryTests {

    @LocalServerPort
    int port;

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @Autowired
    ReactiveElasticsearchClient reactiveElasticsearchClient;

    @Autowired
    ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {

        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
    }

    // New instance is initialized in parent container above. May cause issues later due to test execution order??
    @NestedPerClass
    @DisplayName("Given a new instance of elastic search")
    class GivenANewInstance {

        static final String INDEX_NAME1 = "mytestindex1";

        @Test
        @DisplayName("No index should exist yet")
        void no_index_should_exist_yet() {

            var indexExistsRequest = ExistsRequest.of(builder ->
                builder.index(INDEX_NAME1)
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
                    builder.index(INDEX_NAME1)
                );

                response = reactiveElasticsearchClient.indices().create(createIndexRequest)
                    .block();
            }

            @Test
            @DisplayName("The response should be acknowledged and return the index name")
            void the_response_should_be_acknowledged() {

                Assertions.assertThat(response.acknowledged()).isTrue();
                Assertions.assertThat(response.index()).isEqualTo(INDEX_NAME1);
            }

            @Test
            @DisplayName("The index should exist")
            void the_index_should_exist() {

                var indexExistsRequest = ExistsRequest.of(builder ->
                    builder.index(INDEX_NAME1)
                );

                var result = reactiveElasticsearchClient.indices().exists(indexExistsRequest)
                    .block();

                Assertions.assertThat(result.value()).isTrue();
            }
        }

        @NestedPerClass
        @DisplayName("When an index with mappings is created")
        class WhenAnIndexWithSettingsIsCreated {

            static final String INDEX_NAME2 = "mytestindex2";

            CreateIndexResponse response;

            @BeforeAll
            void when() {
                var createIndexRequest = CreateIndexRequest.of(builder ->

                    builder.index(INDEX_NAME2).mappings(mappingsBuilder -> {
                            var mappingJson = this.getClass().getResourceAsStream("/elasticsearch/discovery_test_mappings1.json");
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
                Assertions.assertThat(response.index()).isEqualTo(INDEX_NAME2);
            }

            @Test
            @DisplayName("The index should exist")
            void the_index_should_exist() {

                var indexExistsRequest = ExistsRequest.of(builder ->
                    builder.index(INDEX_NAME2)
                );

                var result = reactiveElasticsearchClient.indices().exists(indexExistsRequest)
                    .block();

                Assertions.assertThat(result.value()).isTrue();
            }
        }
    }

    @NestedPerClass
    @DisplayName("Given an index with mappings is created")
    class GivenAnIndexWithSettingsIsCreated {

        static final String INDEX_NAME3 = "mytestindex3";

        @BeforeAll
        void given() {
            var createIndexRequest = CreateIndexRequest.of(builder ->

                builder.index(INDEX_NAME3).mappings(mappingsBuilder -> {
                        var mappingJson = this.getClass().getResourceAsStream("/elasticsearch/discovery_test_mappings1.json");
                        return mappingsBuilder.withJson(mappingJson);
                    }
                )
            );

            reactiveElasticsearchClient.indices().create(createIndexRequest)
                .block();
        }

        @NestedPerClass
        @DisplayName("When a document with a mapped keyword field and mapped text field is saved to the index")
        class WhenADocumentWithAMappedFieldIsIndexed {

            // id, and field1 are mapped in the discovery_test_mappings1.json file
            record ESDocument(String id, String field1) { }
            ESDocument toSave = new ESDocument("ABCDE FGHI", "field value1");
            ESDocument savedEntity;

            @BeforeAll
            void when() throws InterruptedException {

                savedEntity = reactiveElasticsearchOperations.save(toSave, IndexCoordinates.of(INDEX_NAME3))
                    .block();

                // Seems this is needed to give time for the document to be searchable??
                // Maybe something with flush?
                Thread.sleep(1000);
            }


            @Test
            @DisplayName("the returned document should have the same values as the saved document")
            void the_returned_entity_should_have_the_same_values_as_the_saved_entity() {

                Assertions.assertThat(savedEntity).isEqualTo(toSave);
            }

            @Test
            @DisplayName("the document should be searchable by exact match on the keyword field")
            void the_document_should_be_searchable_by_exact_match_on_the_keyword_field() {
                CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(toSave.id()));
                var results = reactiveElasticsearchOperations.search(criteriaQuery, ESDocument.class, IndexCoordinates.of(INDEX_NAME3))
                    .collectList()
                    .block();
                        Assertions.assertThat(results).hasSize(1);
                        Assertions.assertThat(results.get(0).getContent()).isEqualTo(toSave);
            }

            @Test
            @DisplayName("the document should not be searchable by partial match on the keyword field")
            void the_document_should_not_be_searchable_by_partial_match_on_the_keyword_field() {

                var query = new NativeQueryBuilder()
                    .withQuery(q -> q.match(m ->
                        m.field("id").query("ABCDE")
                    )).build();
                reactiveElasticsearchOperations.search(query, ESDocument.class, IndexCoordinates.of(INDEX_NAME3))
                    .collectList()
                    .as(StepVerifier::create)
                    .assertNext(searchHits -> {
                        Assertions.assertThat(searchHits).hasSize(0);
                    })
                    .verifyComplete();
            }

            @Test
            @DisplayName("the document should be searchable by partial match on the text field")
            void the_document_should_be_searchable_by_partial_match_on_the_text_field() {

                var query = new NativeQueryBuilder()
                    .withQuery(q -> q.match(m ->
                        m.field("field1").query("value1")
                    )).build();
                reactiveElasticsearchOperations.search(query, ESDocument.class, IndexCoordinates.of(INDEX_NAME3))
                    .collectList()
                    .as(StepVerifier::create)
                    .assertNext(searchHits -> {
                        Assertions.assertThat(searchHits).hasSize(1);
                        Assertions.assertThat(searchHits.get(0).getContent()).isEqualTo(toSave);
                    })
                    .verifyComplete();
            }

            @NestedPerClass
            @DisplayName("And an alias is assigned to the index")
            class AliasAssignedToIndex {

                static final String ALIAS_NAME = "mytestalias";

                @BeforeAll
                void assignAlias() throws InterruptedException {
                    AliasActions aliasActions = new AliasActions();
                    aliasActions.add(new AliasAction.Add(AliasActionParameters.builder()
                        .withIndices(INDEX_NAME3)
                        .withAliases(ALIAS_NAME).build()));
                    reactiveElasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME3)).alias(aliasActions)
                        .block();

//                    Thread.sleep(10000);
                }

                @Test
                @DisplayName("Then the document should be searchable via alias")
                void then_the_document_should_be_searchable_via_alias() {

                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(toSave.id()));
                    reactiveElasticsearchOperations.search(criteriaQuery, ESDocument.class, IndexCoordinates.of(ALIAS_NAME))
                        .collectList()
                        .as(StepVerifier::create)
                        .assertNext(searchHits -> {
                            Assertions.assertThat(searchHits).hasSize(1);
                            Assertions.assertThat(searchHits.get(0).getContent()).isEqualTo(toSave);
                        })
                        .verifyComplete();
                }
            }
        }
    }


}
