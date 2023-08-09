package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.contented.contented.contentlet.elasticsearch.ElasticSearchConfig.INDEX_PROPERTY_KEY;
import static com.contented.contented.contentlet.elasticsearch.ElasticSearchIndexCreator.MAPPINGS_FILE_PROPERTY_KEY;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.mongoDBContainer;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.startAndRegsiterMongoDBContainer;

@Tag("IntegrationTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletController search indexing tests")
public class ContentletControllerSearchIndexTests extends AbstractContentletControllerTests {

    public static final String INDEX_NAME = "controller-test-index1";

    // ContentletRepository needs a MongoDB to communicate with
    @Container
    static MongoDBContainer mongoDBContainer = mongoDBContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @Autowired
    ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    @Autowired
    ElasticSearchIndexCreator elasticSearchIndexCreator;

    @DynamicPropertySource
    static void registerContainersAndOverrideProperties(DynamicPropertyRegistry registry) {
        startAndRegsiterMongoDBContainer(mongoDBContainer, registry);
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
        registry.add(INDEX_PROPERTY_KEY, () -> INDEX_NAME);
        registry.add(MAPPINGS_FILE_PROPERTY_KEY, () -> "elasticsearch/mappings.json");
    }

    @BeforeAll
    void beforeAll() {
        super.beforeAll();
        elasticSearchIndexCreator.createIndex().block();
    }

    record SomeContentlet(String id, String contentType, String title, String body) {
    }

    @NestedPerClass
    @DisplayName("Given content that is indexed by its identifier was saved")
    class GivenContentIndexedByIdentifier {

        SomeContentlet toSave = new SomeContentlet("contentlet1234", "Blog", "Some title", "Some body");

        @BeforeAll
        void given() throws InterruptedException {
            contentletEndpointClient.put().bodyValue(toSave).exchange().expectStatus().isCreated();
            // TODO: Need a better solution than waiting for ES to synchronize
            Thread.sleep(500);
        }

        @NestedPerClass
        @DisplayName("When a search for any content is performed")
        class WhenSearchForAnyContent {

            List<SearchHit<EntityAsMap>> results;

            @BeforeAll
            void when() throws InterruptedException {
                results = reactiveElasticsearchOperations.search(Query.findAll(), EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                    .collectList()
                    .block();
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
            void when() throws InterruptedException {
                CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(toSave.id()));
                results = reactiveElasticsearchOperations.search(criteriaQuery, EntityAsMap.class, IndexCoordinates.of(INDEX_NAME))
                    .collectList()
                    .block();

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
