package com.contented.contented.contentitem.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.contentitem.rest.AbstractContentItemControllerTests;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.elasticsearch.SearchResponseDeserializer;
import com.contented.contented.contentitem.testutils.NestedPerClass;
import tools.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchUtils.waitForESDocumentCount;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("SearchController basic tests")
public class SearchControllerTests {

    @LocalServerPort
    int port;

    // ContentItemRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
    }

    @Autowired ElasticSearchIndexCreator elasticSearchIndexCreator;

    @Autowired ElasticsearchOperations elasticsearchOperations;

    @Autowired IndexCoordinates indexCoordinates;

    WebTestClient searchEndpointClient;

    WebTestClient contentItemEndpointClient;

    @BeforeAll
    void beforeAll() {
        var baseURL = String.format("http://localhost:%s/%s", port, SearchController.SEARCH_PATH);
        searchEndpointClient = WebTestClient.bindToServer().baseUrl(baseURL).build();

        contentItemEndpointClient = AbstractContentItemControllerTests.createContentItemsEndpointClient(port);

        // Create the index! Otherwise queries just return 0 results
        elasticSearchIndexCreator.createIndex();
    }

    @NestedPerClass
    @DisplayName("POST /withcontent endpoint")
    class WithContentEndpoint {
        @NestedPerClass
        @DisplayName("Given content that is indexed by its identifier was saved")
        class GivenContentIndexedByIdentifier {

            record SomeContent(String id, String contentType, String someOtherField){}

            // Given a body with no id (ids are server-assigned)
            final SomeContent savedContent = new SomeContent(null, "Blog", "Some field value");

            UUID createdId;

            @BeforeAll
            void given() {

                // Could use rest endpoint, or could go directly to service
                createdId = contentItemEndpointClient.post().bodyValue(savedContent)
                    .exchange().expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getId();

                waitForESDocumentCount(elasticsearchOperations, indexCoordinates, createdId.toString(), 1);
            }


            @NestedPerClass
            @DisplayName("When a query by its identifier is given")
            class WhenSearchByIdentifier {

                String queryForContentTemplate = """
                {
                    "query": {
                        "term": {
                            "id": "%s"
                        }
                    }
                }
                """;

                WebTestClient.ResponseSpec response;

                WebTestClient.BodySpec<ExpectedResponseStructure, ?> bodySpec;

                record ExpectedResponseStructure(
                    @JsonDeserialize(using = SearchResponseDeserializer.class) SearchResponse<?> esResponse,
                    List<ContentItemResponseDTO> contentItems
                ){}

                @BeforeAll
                void when() {
                    var queryString = String.format(queryForContentTemplate, createdId.toString());
                    response = searchEndpointClient.post().uri("/withcontent").bodyValue(queryString).exchange();

                    // Calling `expectBody` multiple times has inconsistent results so just do it once.
                    bodySpec = response.expectBody(ExpectedResponseStructure.class);
                }

                @Test
                @DisplayName("it should return a 200 OK status code")
                void it_should_return_a_200_OK_status_code() {
                    response.expectStatus().isOk();
                }

                @Test
                @DisplayName("it should return a response with ElasticSearch 'esResponse' field and 'contentItems' field")
                void it_should_return_the_content_item() {
                    bodySpec.value(value -> {
                        assertThat(value.esResponse()).isNotNull();
                        assertThat(value.contentItems()).isNotNull();
                    });
                }

                @Test
                @DisplayName("the 'esResponse' should contain a hit with the id of the saved content")
                void the_esResponse_should_contain_a_hit_with_the_id_of_the_saved_content() {
                    bodySpec
                            .value(value -> {
                                var esResponse = value.esResponse();
                                assertThat(esResponse.hits().hits()).hasSize(1);
                            });
                }

                @Test
                @DisplayName("the 'contentItems' should contain a contentItem with the id of the saved content")
                void the_content_items_should_contain_a_content_item_with_the_id_of_the_saved_content() {
                    bodySpec
                            .value(value -> {
                                var contentItems = value.contentItems();
                                assertThat(contentItems).hasSize(1);
                                assertThat(contentItems.get(0).getId().toString()).isEqualTo(createdId.toString());
                            });
                }

            }

        }
    }
}
