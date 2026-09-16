package com.contented.contented.elasticsearch;

import com.contented.contented.contentitem.ContentItemRepository;
import com.contented.contented.contentitem.testutils.NoDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentitem.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@NoDatabase
@Testcontainers
@DisplayName("IndexController")
class IndexControllerIT {

    @Container
    static ElasticsearchContainer elasticsearchContainer = elasticsearchContainer();

    // No REST endpoint here touches the database; @NoDatabase keeps it out of the context, so the
    // repository the context still wires is mocked.
    @MockitoBean
    ContentItemRepository contentItemRepository;

    @Autowired
    ElasticSearchIndexCreator elasticSearchIndexCreator;

    @LocalServerPort
    int port;

    RestTestClient indexEndpointClient;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegisterElasticsearchContainer(elasticsearchContainer, registry);
    }

    @BeforeAll
    void beforeAll() {
        var baseUrl = String.format("http://localhost:%s/%s", port, IndexController.INDEX_PATH);
        indexEndpointClient = RestTestClient.bindToServer().baseUrl(baseUrl).build();
    }

    @Nested
    @DisplayName("`PUT /index/create` endpoint")
    class PutCreateEndpoint {

        @Nested
        @DisplayName("Given an index already exists with the configured name")
        class GivenIndexAlreadyExists {

            @BeforeAll
            void given() {
                elasticSearchIndexCreator.createIndex();
            }

            @Nested
            @DisplayName("When the endpoint is called")
            class WhenCalled {

                RestTestClient.ResponseSpec response;

                @BeforeAll
                void when() {
                    response = indexEndpointClient.put().uri("/create").exchange();
                }

                @Test
                @DisplayName("It should return `200 OK` instead of `500`")
                void shouldReturn200InsteadOf500() {
                    response.expectStatus().isOk();
                }

                @Test
                @DisplayName("It should indicate the index already exists")
                void shouldIndicateAlreadyExists() {
                    response.expectBody(String.class)
                        .value(body -> assertThat(body).containsIgnoringCase("already exists"));
                }
            }
        }
    }
}
