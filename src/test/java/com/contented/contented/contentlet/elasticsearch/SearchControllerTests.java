package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import com.contented.contented.contentlet.AbstractContentletControllerTests;
import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.*;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("SearchController basic tests")
public class SearchControllerTests {

    @LocalServerPort
    int port;

    // ContentletRepository needs a MongoDB to communicate with
    @Container
    static MongoDBContainer mongoDBContainer = mongoDBContainer();

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegsiterMongoDBContainer(mongoDBContainer, registry);
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
    }

    WebTestClient searchEndpointClient;

    WebTestClient contentletEndpointClient;

    @BeforeAll
    void beforeAll() {
        var baseURL = String.format("http://localhost:%s/%s", port, SearchController.SEARCH_PATH);
        searchEndpointClient = WebTestClient.bindToServer().baseUrl(baseURL).build();

        contentletEndpointClient = AbstractContentletControllerTests.createContentletsEndpointClient(port);
    }

    @NestedPerClass
    @DisplayName("POST /withcontent endpoint")
    class WithContentEndpoint {
        @NestedPerClass
        @DisplayName("Given content that is indexed by its identifier was saved")
        class GivenContentIndexedByIdentifier {

            record SomeContent(String id, String someOtherField){};

            final SomeContent savedContent = new SomeContent("123XYZ", "Some field value");

            @BeforeAll
            void given() {

                // Could use rest endpoint, or could go directly to service
                contentletEndpointClient.put().bodyValue(savedContent)
                    .exchange().expectStatus().is2xxSuccessful();
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

                @BeforeAll
                void when() {
                    var queryString = String.format(queryForContentTemplate, savedContent.id());
                    response = searchEndpointClient.post().uri("/withcontent").bodyValue(queryString).exchange();
                }

                @Test
                @DisplayName("it should return a 200 OK status code")
                void it_should_return_a_200_OK_status_code() {
                    response.expectStatus().isOk();
                }

                @Test
                @DisplayName("it should return a response with ElasticSearch 'esResponse' field and 'contentlets' field")
                void it_should_return_the_contentlet() {
                    var bodySpec = response.expectBody();
                    bodySpec.jsonPath("$.esResponse").exists();
                    bodySpec.jsonPath("$.contentlets").exists();
                }

            }

        }
    }
}
