package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.contentlet.AbstractContentletControllerTests;
import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.elasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchContainerUtils.startAndRegisterElasticsearchContainer;
import static com.contented.contented.contentlet.testutils.ElasticSearchUtils.waitForESToAffectChanges;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.mongoDBContainer;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.startAndRegsiterMongoDBContainer;
import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
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

    @Autowired ElasticSearchIndexCreator elasticSearchIndexCreator;

    WebTestClient searchEndpointClient;

    WebTestClient contentletEndpointClient;

    @BeforeAll
    void beforeAll() {
        var baseURL = String.format("http://localhost:%s/%s", port, SearchController.SEARCH_PATH);
        searchEndpointClient = WebTestClient.bindToServer().baseUrl(baseURL).build();

        contentletEndpointClient = AbstractContentletControllerTests.createContentletsEndpointClient(port);

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

            final SomeContent savedContent = new SomeContent("123XYZ", "Blog", "Some field value");

            @BeforeAll
            void given() {

                // Could use rest endpoint, or could go directly to service
                contentletEndpointClient.put().bodyValue(savedContent)
                    .exchange().expectStatus().is2xxSuccessful();

                waitForESToAffectChanges();
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
                    List<ContentletEntity> contentlets
                ){}

                @BeforeAll
                void when() {
                    var queryString = String.format(queryForContentTemplate, savedContent.id());
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
                @DisplayName("it should return a response with ElasticSearch 'esResponse' field and 'contentlets' field")
                void it_should_return_the_contentlet() {
                    bodySpec.value(value -> {
                        assertThat(value.esResponse()).isNotNull();
                        assertThat(value.contentlets()).isNotNull();
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
                @DisplayName("the 'contentlets' should contain a contentlet with the id of the saved content")
                void the_contentlets_should_contain_a_contentlet_with_the_id_of_the_saved_content() {
                    bodySpec
                            .value(value -> {
                                var contentlets = value.contentlets();
                                assertThat(contentlets).hasSize(1);
                                assertThat(contentlets.get(0).getId()).isEqualTo(savedContent.id());
                            });
                }

            }

        }
    }
}
