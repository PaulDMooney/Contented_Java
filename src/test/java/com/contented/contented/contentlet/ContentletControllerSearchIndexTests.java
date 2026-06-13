package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.contentlet.testutils.NestedPerClass;
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
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.contented.contented.contentlet.elasticsearch.ElasticSearchConfig.INDEX_PROPERTY_KEY;
import static com.contented.contented.contentlet.elasticsearch.ElasticSearchIndexCreator.MAPPINGS_FILE_PROPERTY_KEY;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchUtils.waitForESDocumentCount;
import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletController search indexing tests")
public class ContentletControllerSearchIndexTests extends AbstractContentletControllerTests {

    public static final String INDEX_NAME = "controller-test-index1";

    // ContentletRepository needs a database to communicate with
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

    record SomeContentlet(String id, String contentType, String title, String body) {
    }

    @NestedPerClass
    @DisplayName("PUT endpoint")
    class PutEndpoint {
        @NestedPerClass
        @DisplayName("Given content that is indexed by its identifier was saved")
        class GivenContentIndexedByIdentifier {

            SomeContentlet toSave = new SomeContentlet("contentlet1234", "Blog", "Some title", "Some body");

            @BeforeAll
            void given() {
                contentletEndpointClient.put().bodyValue(toSave).exchange().expectStatus().isCreated();
                waitForESDocumentCount(elasticsearchOperations, IndexCoordinates.of(INDEX_NAME), toSave.id(), 1);
            }

            @NestedPerClass
            @DisplayName("When a search for any content is performed")
            class WhenSearchForAnyContent {

                List<SearchHit<EntityAsMap>> results;

                @BeforeAll
                void when() {
                    results = elasticsearchOperations.search(Query.findAll(), EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                            .getSearchHits();
                }

                @Test
                @DisplayName("Then at least one hit is returned")
                void thenAtLeastOneHitIsReturned() {
                    Assertions.assertThat(results).isNotEmpty();
                }
            }

            @NestedPerClass
            @DisplayName("When a search is performed by its identifier")
            class WhenSearchByIdentifier {

                List<SearchHit<EntityAsMap>> results;

                @BeforeAll
                void when() {
                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(toSave.id()));
                    results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                            .getSearchHits();

                }

                @Test
                @DisplayName("Then a hit with the same identifier is returned")
                void thenContentIsReturned() {
                    Assertions.assertThat(results).hasSize(1);
                    Assertions.assertThat(results.get(0).getContent()).hasFieldOrPropertyWithValue("id", toSave.id());
                }
            }
        }
    }

    @NestedPerClass
    @DisplayName("DELETE endpoint")
    class DeleteEndpoint {

        @NestedPerClass
        @DisplayName("Given content that is indexed by its identifier was saved")
        class GivenContentIndexedByIdentifier {

            static SomeContentlet toDelete = new SomeContentlet("contentlet1234_deleteme", "Blog", "Delete Me", "Some body");

            static WebTestClient.ResponseSpec response;

            @BeforeAll
            void given() {
                contentletEndpointClient.put().bodyValue(toDelete).exchange().expectStatus().isCreated();
                waitForESDocumentCount(elasticsearchOperations, IndexCoordinates.of(INDEX_NAME), toDelete.id(), 1);
            }

            @NestedPerClass
            @DisplayName("when the content is deleted")
            class AndThenContentIsDeleted {

                @BeforeAll
                void when() {
                    response = contentletEndpointClient.delete().uri("/{id}", toDelete.id()).exchange();

                    waitForESDocumentCount(elasticsearchOperations, IndexCoordinates.of(INDEX_NAME), toDelete.id(), 0);
                }

//                @Disabled
                @Test
                @DisplayName("then the content should not longer be found when searching by its identifier")
                void then_the_content_should_not_longer_be_found() {
                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(toDelete.id()));
                    List<SearchHit<EntityAsMap>> results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                            .getSearchHits();

                    Assertions.assertThat(results).hasSize(0);
                }
            }
        }

    }
}
