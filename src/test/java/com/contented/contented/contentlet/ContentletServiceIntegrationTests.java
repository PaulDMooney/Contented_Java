package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Objects;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchUtils.waitForESDocumentCount;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.mongoDBContainer;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.startAndRegsiterMongoDBContainer;
import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletService Integration Tests")
public class ContentletServiceIntegrationTests {

    @Container
    static MongoDBContainer mongoDBContainer = mongoDBContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @Autowired
    ContentletService contentletService;

    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    ElasticSearchIndexCreator elasticSearchIndexCreator;

    @Autowired
    IndexCoordinates indexCoordinates;

    @DynamicPropertySource
    static void registerContainersAndOverrideProperties(DynamicPropertyRegistry registry) {
        startAndRegsiterMongoDBContainer(mongoDBContainer, registry);
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
    }

    @BeforeAll
    void beforeAll() {
        elasticSearchIndexCreator.createIndex();
    }

    @NestedPerClass
    @DisplayName("save")
    class Save {

        @NestedPerClass
        @DisplayName("Given content that matches criteria for elastic search transformations")
        class SavingContentletWithESTransformations {

            // stName = "Blog" will match criteria for a transformation
            ContentletEntity toSave = new ContentletEntity("1234",
                Map.ofEntries(
                    entry("language", "en"),
                    entry("stName", "Blog"),
                    entry("title", "Blog Title"),
                    entry("slug", "blog-slug"),
                    entry("parentDmsId", "parentDmsIdABCDE")));

            @NestedPerClass
            @DisplayName("when saving contentlet")
            class WhenSavingContentlet {

                @BeforeAll
                void beforeAll() {
                    contentletService.save(toSave);
                    waitForESDocumentCount(elasticsearchOperations, indexCoordinates, toSave.getId(), 1);
                }

                @Test
                @DisplayName("the elasticsearch record's _source should contain the transformations")
                void es_should_contain_transformations() {
                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(toSave.getId()));
                    var results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, indexCoordinates).getSearchHits();

                    var hitSource = Objects.requireNonNull(results).get(0).getContent();

                    // This is an expected result from the Blog transformer.
                    assertThat(hitSource.get("blog.title")).isEqualTo("Blog Title");
                }
            }
        }
    }
}
