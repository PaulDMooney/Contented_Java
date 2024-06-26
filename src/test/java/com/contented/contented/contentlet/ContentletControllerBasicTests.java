package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.contented.contented.contentlet.testutils.StubbingUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.mongoDBContainer;
import static com.contented.contented.contentlet.testutils.MongoDBContainerUtils.startAndRegsiterMongoDBContainer;
import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletController basic tests")
public class ContentletControllerBasicTests extends AbstractContentletControllerTests {

    // ContentletRepository needs a MongoDB to communicate with
    @Container
    static MongoDBContainer mongoDBContainer = mongoDBContainer();

    @MockBean
    ContentletIndexer contentletIndexer;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegsiterMongoDBContainer(mongoDBContainer, registry);
    }

    Mono<ContentletEntity> saveOneContentlet() {
        return contentletRepository.save(new ContentletEntity("Contentlet1"));
    }

    void mockContentletIndexer() {

        // Mock the ContentletIndexer to return the contentlet it receives
        // To avoid setting up ElasticSearch in this test. Is this a good idea?
        StubbingUtils.passThrough_indexContentlet(this.contentletIndexer);
    }

    @Nested
    @DisplayName("PUT endpoint")
    class PutEndPoint {

        @NestedPerClass
        @DisplayName("when saving a new contentlet")
        class SaveANewContentlet {

            // Given
            ContentletDTO toSave = new ContentletDTO("Contentlet2");

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentletIndexer();
                // When
                response = contentletEndpointClient.put().bodyValue(toSave).exchange();
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
                    mockContentletIndexer();
                    // When
                    response = contentletEndpointClient.put().bodyValue(toSave).exchange();
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

    @Nested
    @DisplayName("GET /all endpoint")
    class GetALLEndpoint {

        @Test
        @DisplayName("should return all saved contentlets")
        void should_return_all_saved_contentlets() {

            // Given
            saveOneContentlet().block();

            // When
            var response = contentletEndpointClient.get().uri("/all").exchange();

            // Then
            response.expectStatus()
                    .is2xxSuccessful();
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("GET /{id} endpoint")
    class GetByIdEndpoint {

        ContentletEntity savedContentlet = new ContentletEntity("Contentlet4");

        @BeforeAll
        void beforeAll() {

            // Given contentlet is saved
            contentletRepository.save(savedContentlet).block();
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("when querying existing content by id")
        class ExistingContent {

            WebTestClient.ResponseSpec response;

            @BeforeAll
            void beforeAll() {

                // When
                response = contentletEndpointClient.get().uri("/" + savedContentlet.getId()).exchange();
            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void it_should_return_a_200_OK_status_code() {

                // Then
                response.expectStatus()
                        .isOk();
            }

            @Test
            @DisplayName("it should return the contentlet for that id")
            void it_should_return_the_contentlet_for_that_id() {
                // Then
                response.expectBody(ContentletDTO.class)
                        .value(contentletDTO -> {
                            // TODO: More detailed assert that includes checks of more fields.
                            assertThat(contentletDTO.getId()).isEqualTo(savedContentlet.getId());
                        });
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("when querying non-existent content by id")
        class NonExistentContent {

            WebTestClient.ResponseSpec response;

            @BeforeAll
            void beforeAll() {

                // When
                response = contentletEndpointClient.get().uri("/some-non-existent-id").exchange();
            }

            @Test
            @DisplayName("it should return a 404")
            void it_should_return_a_404() {

                // Then
                response.expectStatus().isNotFound();
            }
        }
    }

    @Nested
    @DisplayName("DELETE endpoint")
    class DeleteEndPoint {

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("Given content which already exists in the database")
        class DeleteAContentlet {

            // Given
            static ContentletEntity toDelete = new ContentletEntity("Contentlet3");

            static WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {

                mockContentletIndexer();
                StubbingUtils.passThrough_deleteRecord(contentletIndexer);

                // Save the contentlet to the database
                contentletRepository.save(toDelete).block();

                // When
                response = contentletEndpointClient.delete().uri("/{id}", toDelete.getId()).exchange();
            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void should_return_a_200_OK_status_code() {

                // Then
                response.expectStatus()
                        .isOk();
            }

            @Test
            @DisplayName("it should have deleted the contentlet from the database")
            void should_have_deleted_the_contentlet_from_the_database() {

                // Then contentletRepository should not return the deleted contentlet
                StepVerifier.create(contentletRepository.findById(toDelete.getId()))
                        .expectNextCount(0)
                        .verifyComplete();

            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("Given content which does not exist in the database")
        class DeleteNonExistentContentlet {

            // Given
            static WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {

                mockContentletIndexer();
                StubbingUtils.passThrough_deleteRecord(contentletIndexer);

                // When
                response = contentletEndpointClient.delete().uri("/some-non-existent-id").exchange();
            }

            // TODO: Should update to return a 204
            @Test
            @DisplayName("it should return a 200 status code")
            void should_return_a_200_status_code() {

                // Then
                response.expectStatus()
                    .isOk();
            }
        }
    }
}
