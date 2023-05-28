package com.contented.contented.contentlet.elasticsearch;

import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

    @NestedPerClass
    @DisplayName("Given content that is indexed by its identifier was saved")
    class GivenContentIndexedByIdentifier {

        @Nested
        @DisplayName("When a search is performed by its identifier")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenSearchByIdentifier {

            @BeforeAll
            void when() {


            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void it_should_return_a_200_OK_status_code() {

            }

            @Test
            @DisplayName("it should return the contentlet")
            void it_should_return_the_contentlet() {

            }

        }

    }
}
