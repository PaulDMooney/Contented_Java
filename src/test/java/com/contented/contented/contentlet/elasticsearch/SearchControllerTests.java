package com.contented.contented.contentlet.elasticsearch;

import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @BeforeAll
    void beforeAll() {
        var baseURL = String.format("http://localhost:%s/%s", port, SearchController.SEARCH_PATH);
        searchEndpointClient = WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

    @Disabled
    @NestedPerClass
    @DisplayName("Given content that is indexed by its identifier was saved")
    class GivenContentIndexedByIdentifier {


        @NestedPerClass
        @DisplayName("When a search is performed by its identifier")
        class WhenSearchByIdentifier {

            @BeforeAll
            void when() {


            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void it_should_return_a_200_OK_status_code() {

            }

            @Ignore
            @Test
            @DisplayName("it should return the contentlet")
            void it_should_return_the_contentlet() {

            }

        }

    }
}
