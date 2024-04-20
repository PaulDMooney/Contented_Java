package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperationVariant;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.util.List;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchUtils.waitForESToAffectChanges;
import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers()
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

    @Autowired
    ReactiveElasticsearchTemplate reactiveElasticsearchTemplate;

    @Autowired
    ElasticsearchConverter converter;

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
            void when() {

                savedEntity = reactiveElasticsearchOperations.save(toSave, IndexCoordinates.of(INDEX_NAME3))
                    .block();

                waitForESToAffectChanges();
            }


            @Test
            @DisplayName("the returned document should have the same values as the saved document")
            void the_returned_entity_should_have_the_same_values_as_the_saved_entity() {

                Assertions.assertThat(savedEntity).isEqualTo(toSave);
            }

            @Test
            @DisplayName("the returned document is not just the same object reference as the saved document")
            void the_returned_entity_is_not_just_the_same_object_as_the_saved_entity() {

                Assertions.assertThat(savedEntity).isNotSameAs(toSave);
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

            @Test
            @DisplayName("the document should be searchable by json query on the id field")
            void the_document_should_be_searchable_by_json_query_on_the_id_field() {

                // This is what would be inside the "query" object.
                var queryTemplate = """
                    {
                        "term": {
                            "id": "%s"
                        }
                    }
                    """;
                var query = new StringQuery(String.format(queryTemplate,toSave.id()));
                reactiveElasticsearchOperations.search(query, ESDocument.class, IndexCoordinates.of(INDEX_NAME3))
                    .collectList()
                    .as(StepVerifier::create)
                    .assertNext(searchHits -> {
                        Assertions.assertThat(searchHits).size().isGreaterThan(0);
                    })
                    .verifyComplete();
            }

            @Test
            @DisplayName("the document should be searchable by full JSON query request")
            void the_document_should_be_searchable_by_full_json_query_request() {

                // This is the "full" query like we would use if we were going
                // directly to the elasticsearch /_search endpoint
                var queryStringTemplate = """
                    {
                        "query": {
                            "term": {
                                "id": "%s"
                            }
                        }
                    }
                    """;
                var queryString = String.format(queryStringTemplate, toSave.id());
                SearchRequest.Builder builder = new SearchRequest.Builder();
                builder.withJson(new ByteArrayInputStream(queryString.getBytes()));
                SearchRequest searchRequest = builder.index(INDEX_NAME3).build();

                reactiveElasticsearchClient.search(searchRequest, EntityAsMap.class)
                    .as(StepVerifier::create)
                    .assertNext(response -> {
                        Assertions.assertThat(response.hits().total().value()).isGreaterThan(0);
                        System.out.println(response);
                    })
                    .verifyComplete();
            }

            @NestedPerClass
            @DisplayName("And an alias is assigned to the index")
            class AliasAssignedToIndex {

                static final String ALIAS_NAME = "mytestalias";

                @BeforeAll
                void assignAlias() {
                    AliasActions aliasActions = new AliasActions();
                    aliasActions.add(new AliasAction.Add(AliasActionParameters.builder()
                        .withIndices(INDEX_NAME3)
                        .withAliases(ALIAS_NAME).build()));
                    reactiveElasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME3)).alias(aliasActions)
                        .block();

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

        @NestedPerClass
        @DisplayName("When a document with a mapped keyword field and mapped text field is saved via bulk operations")
        class WhenADocumentWithAMappedFieldIsBulkIndexed {

            // id, and field1 are mapped in the discovery_test_mappings1.json file
            record ESDocument(String id, String field1) { }
            ESDocument toSave = new ESDocument("FGHIABCDE1234", "field value2");
            @BeforeAll
            void when() {

                BulkOperationVariant operation = new IndexOperation.Builder<ESDocument>()
                        .document(toSave)
                        .id(toSave.id())
                    .build();

                var request = new BulkRequest.Builder()

                        .operations(List.of(new BulkOperation(operation)))
                        .index(INDEX_NAME3)
                        .build();

                reactiveElasticsearchClient.bulk(request).block();

                waitForESToAffectChanges();
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
        }
    }


}
