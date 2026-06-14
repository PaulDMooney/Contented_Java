package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import com.contented.contented.contentlet.testutils.StubbingUtils;
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

import static com.contented.contented.contentlet.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;
import static com.contented.contented.contentlet.testutils.PostgresContainerUtils.*;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletController field tests")
public class ContentletControllerFieldTests extends AbstractContentletControllerTests {

    // ContentletRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @MockitoBean
    ContentletIndexer contentletIndexer;

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
        @DisplayName("when creating a contentlet with fields")
        class CreateANewContentlet {

            // Given a body with no id (ids are server-assigned)
            SomethingThatLooksLikeAContentlet toSave =
                new SomethingThatLooksLikeAContentlet(null, "field1Value", 123);

            UUID createdId;

            @BeforeAll
            void when() {

                // Not concerned with indexing, mock the indexer to just pass through
                StubbingUtils.passThrough_indexContentlet(contentletIndexer);

                // When
                createdId = contentletEndpointClient.post().bodyValue(toSave).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentletEntity.class)
                    .returnResult().getResponseBody().getId();
            }

            @Test
            @DisplayName("it should save the contentlet with its given fields")
            void it_should_save_the_contentlet_with_its_given_fields() {

                ContentletEntity savedEntity = contentletRepository.findById(createdId).orElseThrow();

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
        @DisplayName("given a contentlet with fields was saved")
        class GivenAContentletWithFieldsWasSaved {
            // Given a body with no id (ids are server-assigned)
            SomethingThatLooksLikeAContentlet toSave =
                new SomethingThatLooksLikeAContentlet(null, "field1Value", 123);

            UUID createdId;

            @BeforeAll
            void beforeAll() {

                // Not concerned with indexing, mock the indexer to just pass through
                StubbingUtils.passThrough_indexContentlet(contentletIndexer);

                // When
                createdId = contentletEndpointClient.post().bodyValue(toSave).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentletEntity.class)
                    .returnResult().getResponseBody().getId();
            }

            @Nested
            @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            @DisplayName("when getting that contentlet with fields")
            class GetAContentlet {

                WebTestClient.ResponseSpec response;

                @BeforeAll
                void beforeAll() {

                    // Not concerned with indexing, mock the indexer to just pass through
                    StubbingUtils.passThrough_indexContentlet(contentletIndexer);

                    // When
                    response = contentletEndpointClient.get()
                        .uri("/" + createdId)
                        .exchange();
                }

                @Test
                @DisplayName("it should return the contentlet with its fields")
                void it_should_return_the_contentlet_with_its_fields() {

                    // Then
                    response.expectStatus().is2xxSuccessful()
                        .expectBody(SomethingThatLooksLikeAContentlet.class)
                        .value(contentlet -> {
                            assertThat(contentlet.id()).isEqualTo(createdId.toString());
                            assertThat(contentlet.field1()).isEqualTo(toSave.field1());
                            assertThat(contentlet.field2()).isEqualTo(toSave.field2());
                        });
                }
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("given a contentlet with complex fields was saved")
        class GivenAContentletWithComplexFieldsWasSaved {

            record ContentletWithComplexFields(String id, List<String> strings, List<ComplexField> stuff) {
            }

            record ComplexField(String field1, int field2) {
            }

            ContentletWithComplexFields toSave = new ContentletWithComplexFields(
                null,
                List.of("string1", "string2"),
                List.of(new ComplexField("field1Value", 123), new ComplexField("field2Value", 456))
            );

            UUID createdId;

            @BeforeAll
            void given() {

                // Not concerned with indexing, mock the indexer to just pass through
                StubbingUtils.passThrough_indexContentlet(contentletIndexer);

                // Given a body with no id (ids are server-assigned)
                createdId = contentletEndpointClient.post().bodyValue(toSave).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentletEntity.class)
                    .returnResult().getResponseBody().getId();

            }

            @Nested
            @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            @DisplayName("when getting that contentlet with complex fields")
            class GetContentletWithComplexFields {

                WebTestClient.ResponseSpec response;

                @BeforeAll
                void when() {

                    // Not concerned with indexing, mock the indexer to just pass through
                    StubbingUtils.passThrough_indexContentlet(contentletIndexer);

                    // When
                    response = contentletEndpointClient.get()
                        .uri("/" + createdId)
                        .exchange();

                }

                @Test
                @DisplayName("it should return the contentlet with its complex fields")
                void it_should_return_the_contentlet_with_its_complex_fields() {

                    // Then
                    response.expectStatus().is2xxSuccessful()
                        .expectBody(ContentletWithComplexFields.class)
                        .value(contentlet -> {
                            assertThat(contentlet.id()).isEqualTo(createdId.toString());
                            assertThat(contentlet.strings()).isEqualTo(toSave.strings());
                            assertThat(contentlet.stuff()).isEqualTo(toSave.stuff());
                        });
                }
            }

        }

    }

    record SomethingThatLooksLikeAContentlet(String id, String field1, int field2) {
    }
}
