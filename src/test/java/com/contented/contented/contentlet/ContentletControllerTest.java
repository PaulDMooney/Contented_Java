package com.contented.contented.contentlet;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ContentletControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    ContentletRepository contentletRepository;

    // ContentletRepository needs a MongoDB to communicate with
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withExposedPorts(27017);

    WebTestClient webTestClient;

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {

        mongoDBContainer.start();
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    @BeforeEach
    void beforeEach() {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        webTestClient = WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

    Mono<ContentletEntity> saveOneContentlet() {
        return contentletRepository.save(new ContentletEntity("12345"));
    };

    @Nested
    @DisplayName("GET /all endpoint")
    class GetALLEndpoint {

        @Test
        @DisplayName("should return all saved contentlets")
        void should_return_all_saved_contentlets() {

            // Given
            saveOneContentlet().block();

            // When
            var response = webTestClient.get().uri("/all").exchange();

            // Then
            response.expectStatus()
                    .is2xxSuccessful();
        }
    }
}
