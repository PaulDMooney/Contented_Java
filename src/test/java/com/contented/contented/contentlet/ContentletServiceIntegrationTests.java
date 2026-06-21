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
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Objects;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchUtils.waitForESDocumentCount;
import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
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
    static PostgreSQLContainer postgres = postgresContainer();

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
        startAndRegisterPostgresContainer(postgres, registry);
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

            // contentType = "Blog" will match criteria for a transformation
            ContentletDTO toSave = newBlogDTO();

            static ContentletDTO newBlogDTO() {
                var dto = new ContentletDTO();
                dto.setContentType("Blog");
                dto.add("language", "en");
                dto.add("title", "Blog Title");
                dto.add("slug", "blog-slug");
                return dto;
            }

            @NestedPerClass
            @DisplayName("when saving contentlet")
            class WhenSavingContentlet {

                ContentletResponseDTO created;

                @BeforeAll
                void beforeAll() {
                    created = contentletService.create(toSave);
                    waitForESDocumentCount(elasticsearchOperations, indexCoordinates, created.getId().toString(), 1);
                }

                @Test
                @DisplayName("the elasticsearch record's _source should contain the transformations")
                void es_should_contain_transformations() {
                    CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(created.getId().toString()));
                    var results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, indexCoordinates).getSearchHits();

                    var hitSource = Objects.requireNonNull(results).get(0).getContent();

                    // This is an expected result from the Blog transformer.
                    assertThat(hitSource.get("blog.title")).isEqualTo("Blog Title");
                }
            }
        }
    }
}
