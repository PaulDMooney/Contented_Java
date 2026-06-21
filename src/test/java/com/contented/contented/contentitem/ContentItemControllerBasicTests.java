package com.contented.contented.contentitem;

import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import com.contented.contented.contentitem.testutils.NestedPerClass;
import com.contented.contented.contentitem.testutils.StubbingUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;
import java.util.UUID;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentItemController basic tests")
public class ContentItemControllerBasicTests extends AbstractContentItemControllerTests {

    // ContentItemRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @MockitoBean
    ContentItemIndexer contentItemIndexer;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
    }

    ContentItemEntity saveOneContentItem() {
        return contentItemRepository.save(new ContentItemEntity(UuidV7.generate(), "SomeType", Map.of()));
    }

    void mockContentItemIndexer() {

        // Mock the ContentItemIndexer to return the contentItem it receives
        // To avoid setting up ElasticSearch in this test. Is this a good idea?
        StubbingUtils.passThrough_indexContentItem(this.contentItemIndexer);
    }

    @Nested
    @DisplayName("POST endpoint")
    class PostEndPoint {

        @NestedPerClass
        @DisplayName("when creating a new contentItem")
        class CreateANewContentItem {

            // Given a body with no id (ids are server-assigned)
            ContentItemDTO toCreate = new ContentItemDTO();

            EntityExchangeResult<ContentItemResponseDTO> result;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                toCreate.setContentType("SomeType");
                toCreate.add("field1", "value1");

                // When
                result = contentItemEndpointClient.post()
                        .bodyValue(toCreate)
                        .exchange()
                        .expectBody(ContentItemResponseDTO.class)
                        .returnResult();
            }

            @Test
            @DisplayName("it should return a 201 CREATED status code")
            void should_return_a_201_CREATED_status_code() {
                assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
            }

            @Test
            @DisplayName("it should return a Location header for the new contentItem")
            void should_return_a_location_header() {
                assertThat(result.getResponseHeaders().getLocation()).isNotNull();
            }

            @Test
            @DisplayName("it should return the contentItem with a generated id")
            void should_return_a_generated_id() {
                assertThat(result.getResponseBody().getId()).isNotNull();
            }

            @Test
            @DisplayName("it should have saved the contentItem to the database")
            void should_have_saved_the_content_item_to_the_database() {
                var savedContentItem = contentItemRepository.findById(result.getResponseBody().getId());

                assertThat(savedContentItem).isPresent();
            }
        }

        @NestedPerClass
        @DisplayName("when creating a contentItem with a client-supplied id")
        class CreateWithSuppliedId {

            ContentItemDTO toCreate = new ContentItemDTO(UuidV7.generate());

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                // When
                response = contentItemEndpointClient.post().bodyValue(toCreate).exchange();
            }

            @Test
            @DisplayName("it should return a 400 BAD REQUEST status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("it should return a problem detail body")
            void should_return_a_problem_detail_body() {
                response.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(400)
                        .jsonPath("$.detail").exists();
            }
        }

        @NestedPerClass
        @DisplayName("when creating a contentItem with no contentType")
        class CreateWithoutContentType {

            // Given a body with no contentType
            ContentItemDTO toCreate = new ContentItemDTO();

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                toCreate.add("field1", "value1");

                // When
                response = contentItemEndpointClient.post().bodyValue(toCreate).exchange();
            }

            @Test
            @DisplayName("it should return a 400 BAD REQUEST status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("it should return a problem detail body")
            void should_return_a_problem_detail_body() {
                response.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(400)
                        .jsonPath("$.detail").exists();
            }
        }
    }

    @Nested
    @DisplayName("PUT /{id} endpoint")
    class PutEndPoint {

        @NestedPerClass
        @DisplayName("when updating a contentItem that exists")
        class UpdateExistingContentItem {

            // Given an existing contentItem
            ContentItemEntity existing = new ContentItemEntity(UuidV7.generate(), "SomeType", Map.of());

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                contentItemRepository.save(existing);

                ContentItemDTO update = new ContentItemDTO(existing.getId());
                update.setContentType("SomeType");
                update.add("field1", "updatedValue");

                // When
                response = contentItemEndpointClient.put()
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
            @DisplayName("it should have updated the contentItem in the database")
            void should_have_updated_the_content_item() {
                var savedContentItem = contentItemRepository.findById(existing.getId());

                assertThat(savedContentItem).isPresent();
                assertThat((String) savedContentItem.get().get("field1")).isEqualTo("updatedValue");
            }
        }

        @NestedPerClass
        @DisplayName("when updating a contentItem that does not exist")
        class UpdateNonExistentContentItem {

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                UUID id = UuidV7.generate();

                // A valid body so this exercises the not-found path, not contentType validation
                ContentItemDTO update = new ContentItemDTO(id);
                update.setContentType("SomeType");

                // When
                response = contentItemEndpointClient.put()
                        .uri("/{id}", id)
                        .bodyValue(update)
                        .exchange();
            }

            @Test
            @DisplayName("it should return a 404 NOT FOUND status code")
            void should_return_a_404() {
                response.expectStatus().isNotFound();
            }

            @Test
            @DisplayName("it should return a problem detail body")
            void should_return_a_problem_detail_body() {
                response.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(404)
                        .jsonPath("$.detail").exists();
            }
        }

        @NestedPerClass
        @DisplayName("when the body id does not match the URL id")
        class UpdateWithMismatchedBodyId {

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();

                // When the body carries a different id than the URL
                response = contentItemEndpointClient.put()
                        .uri("/{id}", UuidV7.generate())
                        .bodyValue(new ContentItemDTO(UuidV7.generate()))
                        .exchange();
            }

            @Test
            @DisplayName("it should return a 400 BAD REQUEST status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("it should return a problem detail body")
            void should_return_a_problem_detail_body() {
                response.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(400)
                        .jsonPath("$.detail").exists();
            }
        }

        @NestedPerClass
        @DisplayName("when changing the contentType of an existing contentItem")
        class UpdateChangingContentType {

            // Given an existing contentItem whose contentType is "Blog"
            ContentItemEntity existing = new ContentItemEntity(UuidV7.generate(), "Blog", Map.of());

            WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                contentItemRepository.save(existing);

                ContentItemDTO update = new ContentItemDTO(existing.getId());
                update.setContentType("News");

                // When the update supplies a different contentType
                response = contentItemEndpointClient.put()
                        .uri("/{id}", existing.getId())
                        .bodyValue(update)
                        .exchange();
            }

            @Test
            @DisplayName("it should return a 400 BAD REQUEST status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }

            @Test
            @DisplayName("it should return a problem detail body")
            void should_return_a_problem_detail_body() {
                response.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(400)
                        .jsonPath("$.detail").exists();
            }
        }
    }

    @Nested
    @DisplayName("GET /all endpoint")
    class GetALLEndpoint {

        @Test
        @DisplayName("should return all saved contentItems")
        void should_return_all_saved_content_items() {

            // Given
            saveOneContentItem();

            // When
            var response = contentItemEndpointClient.get().uri("/all").exchange();

            // Then
            response.expectStatus()
                    .is2xxSuccessful();
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("GET /{id} endpoint")
    class GetByIdEndpoint {

        ContentItemEntity savedContentItem = new ContentItemEntity(UuidV7.generate(), "SomeType", Map.of());

        @BeforeAll
        void beforeAll() {

            // Given contentItem is saved
            contentItemRepository.save(savedContentItem);
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("when querying existing content by id")
        class ExistingContent {

            WebTestClient.ResponseSpec response;

            @BeforeAll
            void beforeAll() {

                // When
                response = contentItemEndpointClient.get().uri("/" + savedContentItem.getId()).exchange();
            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void it_should_return_a_200_OK_status_code() {

                // Then
                response.expectStatus()
                        .isOk();
            }

            @Test
            @DisplayName("it should return the contentItem for that id")
            void it_should_return_the_content_item_for_that_id() {
                // Then
                response.expectBody(ContentItemResponseDTO.class)
                        .value(contentItemDTO -> {
                            // TODO: More detailed assert that includes checks of more fields.
                            assertThat(contentItemDTO.getId()).isEqualTo(savedContentItem.getId());
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
                response = contentItemEndpointClient.get().uri("/" + UuidV7.generate()).exchange();
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
        class DeleteAContentItem {

            // Given
            static ContentItemEntity toDelete = new ContentItemEntity(UuidV7.generate(), "SomeType", Map.of());

            static WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {

                mockContentItemIndexer();
                StubbingUtils.passThrough_deleteRecord(contentItemIndexer);

                // Save the contentItem to the database
                contentItemRepository.save(toDelete);

                // When
                response = contentItemEndpointClient.delete().uri("/{id}", toDelete.getId()).exchange();
            }

            @Test
            @DisplayName("it should return a 200 OK status code")
            void should_return_a_200_OK_status_code() {

                // Then
                response.expectStatus()
                        .isOk();
            }

            @Test
            @DisplayName("it should have deleted the contentItem from the database")
            void should_have_deleted_the_content_item_from_the_database() {

                // Then contentItemRepository should not return the deleted contentItem
                assertThat(contentItemRepository.findById(toDelete.getId())).isEmpty();
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("Given content which does not exist in the database")
        class DeleteNonExistentContentItem {

            // Given
            static WebTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {

                mockContentItemIndexer();
                StubbingUtils.passThrough_deleteRecord(contentItemIndexer);

                // When
                response = contentItemEndpointClient.delete().uri("/" + UuidV7.generate()).exchange();
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
