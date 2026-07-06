package com.contented.contented.contentitem.rest;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.elasticsearch.ElasticSearchIndexCreator;
import com.contented.contented.elasticsearch.SearchResponseDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
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
@Testcontainers
@DisplayName("`SearchController` basic tests")
public class SearchControllerIT {

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

    RestTestClient searchEndpointClient;

    RestTestClient contentItemEndpointClient;

    @BeforeAll
    void beforeAll() {
        var baseURL = String.format("http://localhost:%s/%s", port, SearchController.SEARCH_PATH);
        searchEndpointClient = RestTestClient.bindToServer().baseUrl(baseURL).build();

        contentItemEndpointClient = AbstractContentItemControllerIT.createContentItemsEndpointClient(port);

        // Create the index! Otherwise queries just return 0 results
        elasticSearchIndexCreator.createIndex();
    }

    @Nested
    @DisplayName("`POST /withcontent` endpoint")
    class WithContentEndpoint {
        @Nested
        @DisplayName("Given published content that is indexed by its `identifier`")
        class GivenContentIndexedByIdentifier {

            // A contentType plus schemaless content nested under `data`.
            final Map<String, Object> savedContent =
                Map.of("contentType", "Blog", "data", Map.of("someOtherField", "Some field value"));

            UUID identifier;

            @BeforeAll
            void given() {

                identifier = contentItemEndpointClient.post().body(savedContent)
                    .exchange().expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getIdentifier();

                // Drafts are not indexed; publishing makes the live version searchable.
                contentItemEndpointClient.post().uri("/{identifier}/publish", identifier).exchange()
                    .expectStatus().isOk();

                waitForESDocumentCount(elasticsearchOperations, indexCoordinates, identifier.toString(), 1);
            }


            @Nested
            @DisplayName("When a query by its `identifier` is given")
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

                RestTestClient.ResponseSpec response;

                ExpectedResponseStructure result;

                record ExpectedResponseStructure(
                    SearchResponse<?> esResponse,
                    List<ContentItemResponseDTO> contentItems
                ){}

                @BeforeAll
                void when() {
                    var queryString = String.format(queryForContentTemplate, identifier.toString());
                    response = searchEndpointClient.post().uri("/withcontent").body(queryString).exchange();

                    // Deserialize the body ourselves with the SearchResponseDeserializer registered as a
                    // module (built with a real JsonpMapper) rather than relying on a field-level
                    // @JsonDeserialize, which would reflectively instantiate it with a default mapper.
                    var body = response.expectBody(String.class).returnResult().getResponseBody();
                    var module = new SimpleModule();
                    module.addDeserializer(SearchResponse.class, new SearchResponseDeserializer<>(new Jackson3JsonpMapper()));
                    var mapper = JsonMapper.builder().addModule(module).build();
                    result = mapper.readValue(body, ExpectedResponseStructure.class);
                }

                @Test
                @DisplayName("It should return a `200 OK` status code")
                void it_should_return_a_200_OK_status_code() {
                    response.expectStatus().isOk();
                }

                @Test
                @DisplayName("It should return a response with an `esResponse` field and a `contentItems` field")
                void it_should_return_the_content_item() {
                    assertThat(result.esResponse()).isNotNull();
                    assertThat(result.contentItems()).isNotNull();
                }

                @Test
                @DisplayName("It should return an `esResponse` with a hit for the published content")
                void the_esResponse_should_contain_a_hit_with_the_id_of_the_saved_content() {
                    assertThat(result.esResponse().hits().hits()).hasSize(1);
                }

                @Test
                @DisplayName("It should hydrate the `contentItems` with the live version of the saved content")
                void the_content_items_should_contain_a_content_item_with_the_id_of_the_saved_content() {
                    assertThat(result.contentItems()).hasSize(1);
                    assertThat(result.contentItems().get(0).getIdentifier()).isEqualTo(identifier);
                }

            }

        }
    }
}
