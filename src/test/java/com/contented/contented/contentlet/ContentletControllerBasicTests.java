package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.contented.contented.contentlet.testutils.StubbingUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletController basic tests")
public class ContentletControllerBasicTests extends AbstractContentletControllerTests {

    // ContentletRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @MockitoBean
    ContentletIndexer contentletIndexer;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
    }

    ContentletEntity saveOneContentlet() {
        return contentletRepository.save(new ContentletEntity(UuidV7.generate()));
    }

    void mockContentletIndexer() {

        // Mock the ContentletIndexer to return the contentlet it receives
        // To avoid setting up ElasticSearch in this test. Is this a good idea?
        StubbingUtils.passThrough_indexContentlet(this.contentletIndexer);
    }

    @Nested
    @DisplayName("POST endpoint")
    class PostEndPoint {

        @NestedPerClass
        @DisplayName("when creating a new contentlet")
        class CreateANewContentlet {

            // Given a body with no id (ids are server-assigned)
            ContentletDTO toCreate = new ContentletDTO();

            EntityExchangeResult<ContentletEntity> result;

            @BeforeAll()
            void beforeAll() {
                mockContentletIndexer();
                toCreate.add("field1", "value1");

                // When
                result = contentletEndpointClient.post()
                        .bodyValue(toCreate)
                        .exchange()
                        .expectBody(ContentletEntity.class)
                        .returnResult();
            }

            @Test
            @DisplayName("it should return a 201 CREATED status code")
            void should_return_a_201_CREATED_status_code() {
                assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
            }

            @Test
            @DisplayName("it should return a Location header for the new contentlet")
            void should_return_a_location_header() {
                assertThat(result.getResponseHeaders().getLocation()).isNotNull();
            }

            @Test
            @DisplayName("it should return the contentlet with a generated id")
            void should_return_a_generated_id() {
                assertThat(result.getResponseBody().getId()).isNotNull();
            }

            @Test
            @DisplayName("it should have saved the contentlet to the database")
            void should_have_saved_the_contentlet_to_the_database() {
                var savedContentlet = contentletRepository.findById(result.getResponseBody().getId());

                assertThat(savedContentlet).isPresent();
            }
        }

        @NestedPerClass
        @DisplayName("when creating a contentlet with a client-supplied id")
        class CreateWithSuppliedId {

            ContentletDTO toCreate = new ContentletDTO(UuidV7.generate());

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentletIndexer();
                // When
                response = contentletEndpointClient.post().bodyValue(toCreate).exchange();
            }

            @Test
            @DisplayName("it should return a 400 BAD REQUEST status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }
        }
    }

    @Nested
    @DisplayName("PUT /{id} endpoint")
    class PutEndPoint {

        @NestedPerClass
        @DisplayName("when updating a contentlet that exists")
        class UpdateExistingContentlet {

            // Given an existing contentlet
            ContentletEntity existing = new ContentletEntity(UuidV7.generate());

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentletIndexer();
                contentletRepository.save(existing);

                ContentletDTO update = new ContentletDTO(existing.getId());
                update.add("field1", "updatedValue");

                // When
                response = contentletEndpointClient.put()
                        .uri("/{id}", existing.getId())
                        .bodyValue(update)
                        .exchange();
            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void should_return_a_200_OK_status_code() {
                response.expectStatus().isOk();
            }

            @Test
            @DisplayName("it should have updated the contentlet in the database")
            void should_have_updated_the_contentlet() {
                var savedContentlet = contentletRepository.findById(existing.getId());

                assertThat(savedContentlet).isPresent();
                assertThat((String) savedContentlet.get().get("field1")).isEqualTo("updatedValue");
            }
        }

        @NestedPerClass
        @DisplayName("when updating a contentlet that does not exist")
        class UpdateNonExistentContentlet {

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentletIndexer();
                UUID id = UuidV7.generate();

                // When
                response = contentletEndpointClient.put()
                        .uri("/{id}", id)
                        .bodyValue(new ContentletDTO(id))
                        .exchange();
            }

            @Test
            @DisplayName("it should return a 404 NOT FOUND status code")
            void should_return_a_404() {
                response.expectStatus().isNotFound();
            }
        }

        @NestedPerClass
        @DisplayName("when the body id does not match the URL id")
        class UpdateWithMismatchedBodyId {

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentletIndexer();

                // When the body carries a different id than the URL
                response = contentletEndpointClient.put()
                        .uri("/{id}", UuidV7.generate())
                        .bodyValue(new ContentletDTO(UuidV7.generate()))
                        .exchange();
            }

            @Test
            @DisplayName("it should return a 400 BAD REQUEST status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
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
            saveOneContentlet();

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

        ContentletEntity savedContentlet = new ContentletEntity(UuidV7.generate());

        @BeforeAll
        void beforeAll() {

            // Given contentlet is saved
            contentletRepository.save(savedContentlet);
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
                response = contentletEndpointClient.get().uri("/" + UuidV7.generate()).exchange();
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
            static ContentletEntity toDelete = new ContentletEntity(UuidV7.generate());

            static WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {

                mockContentletIndexer();
                StubbingUtils.passThrough_deleteRecord(contentletIndexer);

                // Save the contentlet to the database
                contentletRepository.save(toDelete);

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
                assertThat(contentletRepository.findById(toDelete.getId())).isEmpty();
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
                response = contentletEndpointClient.delete().uri("/" + UuidV7.generate()).exchange();
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
