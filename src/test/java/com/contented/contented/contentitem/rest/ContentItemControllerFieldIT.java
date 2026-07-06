package com.contented.contented.contentitem.rest;

import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.model.ContentItemWorkAndLiveDTO;
import com.contented.contented.contentitem.testutils.StubbingUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static org.assertj.core.api.Assertions.assertThat;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.*;

@Tag(INTEGRATION_TESTS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("`ContentItemController` field tests")
public class ContentItemControllerFieldIT extends AbstractContentItemControllerIT {

    // ContentItemRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @MockitoBean
    ContentItemIndexer contentItemIndexer;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
    }

    // The wire shape: a contentType plus the schemaless content nested under `data`.
    static Map<String, Object> requestBody(String contentType, Map<String, Object> data) {
        return Map.of("contentType", contentType, "data", data);
    }

    @Nested
    @DisplayName("`POST` endpoint")
    class PostEndPoint {

        @Nested
        @DisplayName("When creating a `contentItem` with fields")
        class CreateANewContentItem {

            Map<String, Object> data = Map.of("field1", "field1Value", "field2", 123);

            UUID createdVersionId;

            @BeforeAll
            void when() {
                StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                createdVersionId = contentItemEndpointClient.post().body(requestBody("SomeType", data)).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getVersionId();
            }

            @Test
            @DisplayName("It should save the working version with its given fields")
            void it_should_save_the_content_item_with_its_given_fields() {

                ContentItemEntity savedEntity = contentItemRepository.findById(createdVersionId).orElseThrow();

                assertThat((String) savedEntity.get("field1")).isEqualTo("field1Value");
                assertThat((Integer) savedEntity.get("field2")).isEqualTo(123);
            }
        }
    }

    @Nested
    @DisplayName("`GET /{identifier}` endpoint")
    class GetByIdentifierEndPoint {

        @Nested
        @DisplayName("Given a draft `contentItem` with fields was created")
        class GivenAContentItemWithFieldsWasSaved {

            Map<String, Object> data = Map.of("field1", "field1Value", "field2", 123);

            UUID identifier;

            @BeforeAll
            void beforeAll() {
                StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                identifier = contentItemEndpointClient.post().body(requestBody("SomeType", data)).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getIdentifier();
            }

            @Test
            @DisplayName("It should return the working version with its fields")
            void it_should_return_the_content_item_with_its_fields() {

                contentItemEndpointClient.get().uri("/{identifier}", identifier).exchange()
                    .expectStatus().is2xxSuccessful()
                    .expectBody(ContentItemWorkAndLiveDTO.class)
                    .value(state -> {
                        assertThat(state.working().getData()).containsEntry("field1", "field1Value");
                        assertThat(state.working().getData()).containsEntry("field2", 123);
                    });
            }
        }

        @Nested
        @DisplayName("Given a draft `contentItem` with complex fields was created")
        class GivenAContentItemWithComplexFieldsWasSaved {

            List<String> strings = List.of("string1", "string2");
            List<Map<String, Object>> stuff = List.of(
                Map.of("field1", "field1Value", "field2", 123),
                Map.of("field1", "field2Value", "field2", 456));
            Map<String, Object> data = Map.of("strings", strings, "stuff", stuff);

            UUID identifier;

            @BeforeAll
            void given() {
                StubbingUtils.passThrough_indexContentItem(contentItemIndexer);

                identifier = contentItemEndpointClient.post().body(requestBody("SomeType", data)).exchange()
                    .expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .returnResult().getResponseBody().getIdentifier();
            }

            @Test
            @DisplayName("It should return the working version with its complex fields")
            void it_should_return_the_content_item_with_its_complex_fields() {

                contentItemEndpointClient.get().uri("/{identifier}", identifier).exchange()
                    .expectStatus().is2xxSuccessful()
                    .expectBody(ContentItemWorkAndLiveDTO.class)
                    .value(state -> {
                        assertThat(state.working().getData()).containsEntry("strings", strings);
                        assertThat(state.working().getData()).containsKey("stuff");
                    });
            }
        }
    }
}
