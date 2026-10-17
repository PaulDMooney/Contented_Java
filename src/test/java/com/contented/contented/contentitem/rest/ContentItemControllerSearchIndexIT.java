package com.contented.contented.contentitem.rest;

import com.contented.contented.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.contented.contented.elasticsearch.ElasticSearchConfig.INDEX_PROPERTY_KEY;
import static com.contented.contented.elasticsearch.ElasticSearchIndexCreator.MAPPINGS_FILE_PROPERTY_KEY;
import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchUtils.waitForESDocumentCount;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("`ContentItemController` search indexing tests")
public class ContentItemControllerSearchIndexIT extends AbstractContentItemControllerIT {

    public static final String INDEX_NAME = "controller-test-index1";

    // ContentItemRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    ElasticSearchIndexCreator elasticSearchIndexCreator;

    @DynamicPropertySource
    static void registerContainersAndOverrideProperties(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
        registry.add(INDEX_PROPERTY_KEY, () -> INDEX_NAME);
        registry.add(MAPPINGS_FILE_PROPERTY_KEY, () -> "elasticsearch/mappings.json");
    }

    @BeforeAll
    void beforeAll() {
        super.beforeAll();
        elasticSearchIndexCreator.createIndex();
    }

    UUID createAndPublish(String title, String body) {
        var requestBody = Map.of("contentType", "Blog", "data", Map.of("title", title, "body", body));
        var identifier = contentItemEndpointClient.post().body(requestBody).exchange()
            .expectStatus().isCreated()
            .expectBody(ContentItemResponseDTO.class)
            .returnResult().getResponseBody().getIdentifier();
        contentItemEndpointClient.post().uri("/{identifier}/publish", identifier).exchange()
            .expectStatus().isOk();
        return identifier;
    }

    @Nested
    @DisplayName("`POST /{identifier}/publish`")
    class PublishEndpoint {
        @Nested
        @DisplayName("Given content that was published and indexed by its `identifier`")
        class GivenContentIndexedByIdentifier {

            UUID identifier;

            @BeforeAll
            void given() {
                identifier = createAndPublish("Some title", "Some body");
                waitForESDocumentCount(elasticsearchOperations, IndexCoordinates.of(INDEX_NAME), identifier.toString(), 1);
            }

            @Nested
            @DisplayName("When a search for any content is performed")
            class WhenSearchForAnyContent {

                List<SearchHit<EntityAsMap>> results;

                @BeforeAll
                void when() {
                    results = elasticsearchOperations.search(Query.findAll(), EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                            .getSearchHits();
                }

                @Test
                @DisplayName("It should return at least one hit")
                void thenAtLeastOneHitIsReturned() {
                    Assertions.assertThat(results).isNotEmpty();
                }
            }

            @Nested
            @DisplayName("When a search is performed by its `identifier`")
            class WhenSearchByIdentifier {

                List<SearchHit<EntityAsMap>> results;

                @BeforeAll
                void when() {
                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(identifier.toString()));
                    results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                            .getSearchHits();

                }

                @Test
                @DisplayName("It should return a hit keyed on the `identifier`")
                void thenContentIsReturned() {
                    Assertions.assertThat(results).hasSize(1);
                    Assertions.assertThat(results.get(0).getContent()).hasFieldOrPropertyWithValue("id", identifier.toString());
                }
            }
        }
    }

    @Nested
    @DisplayName("`DELETE` endpoint")
    class DeleteEndpoint {

        @Nested
        @DisplayName("Given published content indexed by its `identifier`")
        class GivenContentIndexedByIdentifier {

            UUID identifier;

            @BeforeAll
            void given() {
                identifier = createAndPublish("Delete Me", "Some body");
                waitForESDocumentCount(elasticsearchOperations, IndexCoordinates.of(INDEX_NAME), identifier.toString(), 1);
            }

            @Nested
            @DisplayName("When the content is deleted")
            class AndThenContentIsDeleted {

                @BeforeAll
                void when() {
                    contentItemEndpointClient.delete().uri("/{identifier}", identifier).exchange()
                        .expectStatus().isNoContent();

                    waitForESDocumentCount(elasticsearchOperations, IndexCoordinates.of(INDEX_NAME), identifier.toString(), 0);
                }

                @Test
                @DisplayName("It should no longer be found when searching by its `identifier`")
                void then_the_content_should_not_longer_be_found() {
                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(identifier.toString()));
                    List<SearchHit<EntityAsMap>> results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                            .getSearchHits();

                    Assertions.assertThat(results).hasSize(0);
                }
            }
        }

    }
}
