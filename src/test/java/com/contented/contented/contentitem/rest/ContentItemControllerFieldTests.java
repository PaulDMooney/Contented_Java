package com.contented.contented.contentitem.rest;

import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.testutils.StubbingUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.*;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentItemController field tests")
public class ContentItemControllerFieldTests extends AbstractContentItemControllerTests {

    // ContentItemRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @MockitoBean
    ContentItemIndexer contentItemIndexer;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("POST endpoint")
    class PostEndPoint {

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("when creating a contentItem with fields")
        class CreateANewContentItem {

            // Given a body with no id (ids are server-assigned)
            SomethingThatLooksLikeAContentItem toSave =
                new SomethingThatLooksLikeAContentItem(null, "SomeType", "field1Value", 123);

            UUID createdId;

            @BeforeAll
            void when() {

                // Not concerned with indexing, mock the indexer to just pass through
                StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                // When
                createdId = contentItemEndpointClient.post().bodyValue(toSave).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getId();
            }

            @Test
            @DisplayName("it should save the contentItem with its given fields")
            void it_should_save_the_content_item_with_its_given_fields() {

                ContentItemEntity savedEntity = contentItemRepository.findById(createdId).orElseThrow();

                assertThat((String) savedEntity.get("field1")).isEqualTo(toSave.field1());
                assertThat((Integer) savedEntity.get("field2")).isEqualTo(toSave.field2());
            }
        }
    }

    @Nested
    @DisplayName("GET /{id} endpoint")
    class GetByIdEndPoint {

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("given a contentItem with fields was saved")
        class GivenAContentItemWithFieldsWasSaved {
            // Given a body with no id (ids are server-assigned)
            SomethingThatLooksLikeAContentItem toSave =
                new SomethingThatLooksLikeAContentItem(null, "SomeType", "field1Value", 123);

            UUID createdId;

            @BeforeAll
            void beforeAll() {

                // Not concerned with indexing, mock the indexer to just pass through
                StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                // When
                createdId = contentItemEndpointClient.post().bodyValue(toSave).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getId();
            }

            @Nested
            @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            @DisplayName("when getting that contentItem with fields")
            class GetAContentItem {

                WebTestClient.ResponseSpec response;

                @BeforeAll
                void beforeAll() {

                    // Not concerned with indexing, mock the indexer to just pass through
                    StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                    // When
                    response = contentItemEndpointClient.get()
                        .uri("/" + createdId)
                        .exchange();
                }

                @Test
                @DisplayName("it should return the contentItem with its fields")
                void it_should_return_the_content_item_with_its_fields() {

                    // Then
                    response.expectStatus().is2xxSuccessful()
                        .expectBody(SomethingThatLooksLikeAContentItem.class)
                        .value(contentItem -> {
                            assertThat(contentItem.id()).isEqualTo(createdId.toString());
                            assertThat(contentItem.field1()).isEqualTo(toSave.field1());
                            assertThat(contentItem.field2()).isEqualTo(toSave.field2());
                        });
                }
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("given a contentItem with complex fields was saved")
        class GivenAContentItemWithComplexFieldsWasSaved {

            record ContentItemWithComplexFields(String id, String contentType, List<String> strings, List<ComplexField> stuff) {
            }

            record ComplexField(String field1, int field2) {
            }

            ContentItemWithComplexFields toSave = new ContentItemWithComplexFields(
                null,
                "SomeType",
                List.of("string1", "string2"),
                List.of(new ComplexField("field1Value", 123), new ComplexField("field2Value", 456))
            );

            UUID createdId;

            @BeforeAll
            void given() {

                // Not concerned with indexing, mock the indexer to just pass through
                StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                // Given a body with no id (ids are server-assigned)
                createdId = contentItemEndpointClient.post().bodyValue(toSave).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getId();

            }

            @Nested
            @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            @DisplayName("when getting that contentItem with complex fields")
            class GetContentItemWithComplexFields {

                WebTestClient.ResponseSpec response;

                @BeforeAll
                void when() {

                    // Not concerned with indexing, mock the indexer to just pass through
                    StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                    // When
                    response = contentItemEndpointClient.get()
                        .uri("/" + createdId)
                        .exchange();

                }

                @Test
                @DisplayName("it should return the contentItem with its complex fields")
                void it_should_return_the_content_item_with_its_complex_fields() {

                    // Then
                    response.expectStatus().is2xxSuccessful()
                        .expectBody(ContentItemWithComplexFields.class)
                        .value(contentItem -> {
                            assertThat(contentItem.id()).isEqualTo(createdId.toString());
                            assertThat(contentItem.strings()).isEqualTo(toSave.strings());
                            assertThat(contentItem.stuff()).isEqualTo(toSave.stuff());
                        });
                }
            }

        }

    }

    record SomethingThatLooksLikeAContentItem(String id, String contentType, String field1, int field2) {
    }
}
