package com.contented.contented.contentitem;

import com.contented.contented.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
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

import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchUtils.waitForESDocumentCount;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("`ContentItemService` Integration Tests")
public class ContentItemServiceIT {

    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @Autowired
    ContentItemService contentItemService;

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

    @Nested
    @DisplayName("`publish()`")
    class Publish {

        @Nested
        @DisplayName("When publishing a `contentItem` that matches Elasticsearch transformation criteria")
        class WhenPublishingContentItemMatchingESCriteria {

            // contentType = "Blog" will match criteria for a transformation
            ContentItemDTO toSave = newBlogDTO();

            static ContentItemDTO newBlogDTO() {
                return ContentItemDTO.builder()
                    .contentType("Blog")
                    .data(Map.of("language", "en", "title", "Blog Title", "slug", "blog-slug"))
                    .build();
            }

            ContentItemResponseDTO created;

            @BeforeAll
            void when() {
                created = contentItemService.create(toSave);
                // Drafts are not indexed; publishing makes the live version searchable.
                contentItemService.publish(created.getIdentifier());
                waitForESDocumentCount(elasticsearchOperations, indexCoordinates, created.getIdentifier().toString(), 1);
            }

            @Test
            @DisplayName("It should index the transformed fields into the Elasticsearch record")
            void es_should_contain_transformations() {
                CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("id").is(created.getIdentifier().toString()));
                var results = elasticsearchOperations.search(criteriaQuery, EntityAsMap.class, indexCoordinates).getSearchHits();

                var hitSource = Objects.requireNonNull(results).get(0).getContent();

                // This is an expected result from the Blog transformer.
                assertThat(hitSource.get("blog.title")).isEqualTo("Blog Title");
            }
        }
    }
}
