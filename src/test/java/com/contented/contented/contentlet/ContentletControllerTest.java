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
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    @BeforeAll()
    void beforeEach() {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        webTestClient = WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

    Mono<ContentletEntity> saveOneContentlet() {
        return contentletRepository.save(new ContentletEntity("Contentlet1"));
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

    @Nested
    @DisplayName("PUT endpoint")
    class PutEndPoint {

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("when saving a new contentlet")
        class SaveANewContentlet{

            // Given
            static ContentletDTO toSave = new ContentletDTO("Contentlet2");

            static WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {

                // When
                response = webTestClient.put().bodyValue(toSave).exchange();
            }

            @Test
            @DisplayName("it should return a 201 CREATED status code")
            void should_return_a_201_CREATED_status_code() {

                // Then
                response.expectStatus()
                        .isCreated();
            }

            @Test
            @DisplayName("it should have saved the contentlet to the database")
            void should_have_saved_the_contentlet_to_the_database() {

                // Then contentletRepository should return the saved contentlet
                StepVerifier.create(contentletRepository.findById(toSave.getId()))

                        .expectNextMatches(savedContentlet -> {
                            assertThat(savedContentlet).isNotNull();
                            assertThat(savedContentlet.getId()).isEqualTo(toSave.getId());
                            return true;
                        })
                        .verifyComplete();

            }

            @Nested
            @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            @DisplayName("when saving a contentlet with an existing id")
            class SaveAContentletWithAnExistingId {

                static WebTestClient.ResponseSpec response;

                @BeforeAll()
                void beforeAll() {

                    // When
                    response = webTestClient.put().bodyValue(toSave).exchange();
                }

                @Test
                @DisplayName("it should return a 200 CREATED status code")
                void should_return_a_200_OK_status_code() {

                    // Then
                    response.expectStatus()
                            .isOk();
                }

                @Test
                @DisplayName("it should have saved the contentlet to the database")
                void should_have_saved_the_contentlet_to_the_database() {

                    // Then contentletRepository should return the saved contentlet
                    StepVerifier.create(contentletRepository.findById(toSave.getId()))

                            .expectNextMatches(savedContentlet -> {
                                assertThat(savedContentlet).isNotNull();
                                assertThat(savedContentlet.getId()).isEqualTo(toSave.getId());
                                return true;
                            })
                            .verifyComplete();

                }
            }

        }
    }
}
