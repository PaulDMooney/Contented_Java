package com.contented.contented.contentitem.rest;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.model.ContentItemState;
import com.contented.contented.contentitem.model.ContentItemWorkAndLiveDTO;
import com.contented.contented.contentitem.model.ContentItemVersionSummaryDTO;
import com.contented.contented.contentitem.testutils.StubbingUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
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
@Testcontainers
@DisplayName("`ContentItemController` basic tests")
public class ContentItemControllerBasicIT extends AbstractContentItemControllerIT {

    // ContentItemRepository needs a database to communicate with
    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @MockitoBean
    ContentItemIndexer contentItemIndexer;

    @DynamicPropertySource
    static void startAndRegisterContainers(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
    }

    void mockContentItemIndexer() {
        StubbingUtils.passThrough_indexContentItem(this.contentItemIndexer);
        StubbingUtils.passThrough_deleteRecord(this.contentItemIndexer);
    }

    UUID createDraft(String contentType, Map<String, Object> fields) {
        var dto = ContentItemDTO.builder().contentType(contentType).data(fields).build();
        return contentItemEndpointClient.post().body(dto).exchange()
            .expectStatus().isCreated()
            .expectBody(ContentItemResponseDTO.class)
            .returnResult().getResponseBody().getIdentifier();
    }

    void publish(UUID identifier) {
        contentItemEndpointClient.post().uri("/{identifier}/publish", identifier).exchange()
            .expectStatus().isOk();
    }

    void editWorking(UUID identifier, String contentType, Map<String, Object> fields) {
        var dto = ContentItemDTO.builder().contentType(contentType).data(fields).build();
        contentItemEndpointClient.put().uri("/{identifier}", identifier).body(dto).exchange()
            .expectStatus().isOk();
    }

    @Nested
    @DisplayName("`POST` endpoint")
    class PostEndPoint {

        @Nested
        @DisplayName("When creating a new `contentItem`")
        class CreateANewContentItem {

            // Given a body with no id (ids are server-assigned)
            ContentItemDTO toCreate = ContentItemDTO.builder()
                .contentType("SomeType").data(Map.of("field1", "value1")).build();

            EntityExchangeResult<ContentItemResponseDTO> result;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();

                result = contentItemEndpointClient.post()
                        .body(toCreate)
                        .exchange()
                        .expectBody(ContentItemResponseDTO.class)
                        .returnResult();
            }

            @Test
            @DisplayName("It should return a `201 CREATED` status code")
            void should_return_a_201_CREATED_status_code() {
                assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
            }

            @Test
            @DisplayName("It should return a `Location` header for the new content")
            void should_return_a_location_header() {
                assertThat(result.getResponseHeaders().getLocation()).isNotNull();
            }

            @Test
            @DisplayName("It should return a WORKING draft with a generated version id and identifier")
            void should_return_a_working_draft() {
                assertThat(result.getResponseBody().getVersionId()).isNotNull();
                assertThat(result.getResponseBody().getIdentifier()).isNotNull();
                assertThat(result.getResponseBody().getState()).isEqualTo(ContentItemState.WORKING);
            }
        }

        @Nested
        @DisplayName("When creating a `contentItem` with a stray identity field in the body")
        class CreateWithStrayIdentity {

            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                // Identity is server-assigned; a stray top-level `identifier` is an unknown property
                // and is simply ignored (not bound, not leaked into `data`).
                var body = Map.of(
                    "identifier", UuidV7.generate().toString(),
                    "contentType", "SomeType",
                    "data", Map.of("field1", "value1"));
                response = contentItemEndpointClient.post().body(body).exchange();
            }

            @Test
            @DisplayName("It should create the content, ignoring the supplied identity")
            void should_ignore_supplied_identity() {
                response.expectStatus().isCreated()
                    .expectBody(ContentItemResponseDTO.class)
                    .value(created -> {
                        assertThat(created.getIdentifier()).isNotNull();
                        assertThat(created.getData()).doesNotContainKey("identifier");
                    });
            }
        }

        @Nested
        @DisplayName("When creating a `contentItem` with no `contentType`")
        class CreateWithoutContentType {

            // Given a body with no contentType
            ContentItemDTO toCreate = ContentItemDTO.builder().data(Map.of("field1", "value1")).build();

            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                response = contentItemEndpointClient.post().body(toCreate).exchange();
            }

            @Test
            @DisplayName("It should return a `400 BAD REQUEST` status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }
        }
    }

    @Nested
    @DisplayName("`PUT /{identifier}` endpoint")
    class PutEndPoint {

        @Nested
        @DisplayName("When editing the working draft of existing content")
        class EditExistingWorking {

            UUID identifier;
            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                identifier = createDraft("SomeType", Map.of("field1", "original"));

                var update = ContentItemDTO.builder()
                    .contentType("SomeType").data(Map.of("field1", "updatedValue")).build();

                response = contentItemEndpointClient.put()
                        .uri("/{identifier}", identifier)
                        .body(update)
                        .exchange();
            }

            @Test
            @DisplayName("It should return a `200 OK` status code")
            void should_return_a_200_OK_status_code() {
                response.expectStatus().isOk();
            }

            @Test
            @DisplayName("It should update the working version's fields")
            void should_have_updated_the_working_version() {
                contentItemEndpointClient.get().uri("/{identifier}", identifier).exchange()
                    .expectStatus().isOk()
                    .expectBody(ContentItemWorkAndLiveDTO.class)
                    .value(state -> assertThat(state.working().getData()).containsEntry("field1", "updatedValue"));
            }
        }

        @Nested
        @DisplayName("When editing content whose identifier does not exist")
        class EditNonExistentContent {

            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                var update = ContentItemDTO.builder().contentType("SomeType").build();

                response = contentItemEndpointClient.put()
                        .uri("/{identifier}", UuidV7.generate())
                        .body(update)
                        .exchange();
            }

            @Test
            @DisplayName("It should return a `404 NOT FOUND` status code")
            void should_return_a_404() {
                response.expectStatus().isNotFound();
            }
        }

        @Nested
        @DisplayName("When the `contentType` differs from the existing content's `contentType`")
        class EditChangingContentType {

            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                var identifier = createDraft("Blog", Map.of());

                var update = ContentItemDTO.builder().contentType("News").build();

                response = contentItemEndpointClient.put()
                        .uri("/{identifier}", identifier)
                        .body(update)
                        .exchange();
            }

            @Test
            @DisplayName("It should return a `400 BAD REQUEST` status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }
        }
    }

    @Nested
    @DisplayName("`POST /{identifier}/publish` endpoint")
    class PublishEndPoint {

        @Nested
        @DisplayName("When publishing content that has a working draft")
        class PublishWorking {

            UUID identifier;
            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                identifier = createDraft("SomeType", Map.of("field1", "value1"));
                response = contentItemEndpointClient.post().uri("/{identifier}/publish", identifier).exchange();
            }

            @Test
            @DisplayName("It should return the now-live version")
            void should_return_the_live_version() {
                response.expectStatus().isOk()
                    .expectBody(ContentItemResponseDTO.class)
                    .value(live -> assertThat(live.getState()).isEqualTo(ContentItemState.LIVE));
            }

            @Test
            @DisplayName("It should leave the content with a live version and no working version")
            void should_have_live_and_no_working() {
                contentItemEndpointClient.get().uri("/{identifier}", identifier).exchange()
                    .expectStatus().isOk()
                    .expectBody(ContentItemWorkAndLiveDTO.class)
                    .value(state -> {
                        assertThat(state.live()).isNotNull();
                        assertThat(state.working()).isNull();
                    });
            }
        }

        @Nested
        @DisplayName("When publishing an unknown identifier")
        class PublishUnknown {

            @Test
            @DisplayName("It should return a `404 NOT FOUND` status code")
            void should_return_a_404() {
                mockContentItemIndexer();
                contentItemEndpointClient.post().uri("/{identifier}/publish", UuidV7.generate()).exchange()
                    .expectStatus().isNotFound();
            }
        }

        @Nested
        @DisplayName("When publishing content that has no working version")
        class PublishNothingToPublish {

            RestTestClient.ResponseSpec response;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                var identifier = createDraft("SomeType", Map.of());
                publish(identifier); // consumes the working version
                response = contentItemEndpointClient.post().uri("/{identifier}/publish", identifier).exchange();
            }

            @Test
            @DisplayName("It should return a `400 BAD REQUEST` status code")
            void should_return_a_400() {
                response.expectStatus().isBadRequest();
            }
        }
    }

    @Nested
    @DisplayName("`GET /{identifier}` endpoint")
    class GetByIdentifierEndpoint {

        @Nested
        @DisplayName("When the content has only a working draft")
        class OnlyWorking {

            UUID identifier;

            @BeforeAll
            void beforeAll() {
                mockContentItemIndexer();
                identifier = createDraft("SomeType", Map.of("field1", "value1"));
            }

            @Test
            @DisplayName("It should return an envelope with the working version and a null live version")
            void should_return_working_only() {
                contentItemEndpointClient.get().uri("/{identifier}", identifier).exchange()
                    .expectStatus().isOk()
                    .expectBody(ContentItemWorkAndLiveDTO.class)
                    .value(state -> {
                        assertThat(state.working()).isNotNull();
                        assertThat(state.live()).isNull();
                    });
            }
        }

        @Nested
        @DisplayName("When the identifier is unknown")
        class UnknownIdentifier {

            @Test
            @DisplayName("It should return a `404 NOT FOUND` status code")
            void should_return_a_404() {
                mockContentItemIndexer();
                contentItemEndpointClient.get().uri("/{identifier}", UuidV7.generate()).exchange()
                    .expectStatus().isNotFound();
            }
        }
    }

    @Nested
    @DisplayName("`GET /{identifier}/versions` endpoint")
    class VersionsEndpoint {

        @Nested
        @DisplayName("Given content that has been published and then edited again")
        class GivenPublishedThenEdited {

            UUID identifier;

            @BeforeAll
            void beforeAll() {
                mockContentItemIndexer();
                identifier = createDraft("SomeType", Map.of("field1", "v1"));
                publish(identifier);
                editWorking(identifier, "SomeType", Map.of("field1", "v2"));
            }

            @Test
            @DisplayName("It should return the live and working versions in its history")
            void should_return_history() {
                contentItemEndpointClient.get().uri("/{identifier}/versions", identifier).exchange()
                    .expectStatus().isOk()
                    .expectBody(new org.springframework.core.ParameterizedTypeReference<List<ContentItemVersionSummaryDTO>>() {})
                    .value(versions -> {
                        assertThat(versions).hasSize(2);
                        assertThat(versions).extracting(ContentItemVersionSummaryDTO::state)
                            .containsExactlyInAnyOrder(ContentItemState.WORKING, ContentItemState.LIVE);
                    });
            }
        }
    }

    @Nested
    @DisplayName("`POST /{identifier}/versions/{versionId}/restore` endpoint")
    class RestoreEndpoint {

        @Nested
        @DisplayName("Given content with an archived previous version")
        class GivenArchivedVersion {

            UUID identifier;
            UUID archivedVersionId;

            @BeforeAll
            void beforeAll() {
                mockContentItemIndexer();
                identifier = createDraft("SomeType", Map.of("field1", "original"));
                publish(identifier);
                editWorking(identifier, "SomeType", Map.of("field1", "revised"));
                publish(identifier); // original is now archived

                archivedVersionId = contentItemEndpointClient.get().uri("/{identifier}/versions", identifier).exchange()
                    .expectBody(new org.springframework.core.ParameterizedTypeReference<List<ContentItemVersionSummaryDTO>>() {})
                    .returnResult().getResponseBody().stream()
                    .filter(version -> version.state() == ContentItemState.ARCHIVED)
                    .findFirst().orElseThrow().versionId();
            }

            @Test
            @DisplayName("It should copy the archived content into a new working draft")
            void should_restore_into_working() {
                contentItemEndpointClient.post()
                    .uri("/{identifier}/versions/{versionId}/restore", identifier, archivedVersionId).exchange()
                    .expectStatus().isOk()
                    .expectBody(ContentItemResponseDTO.class)
                    .value(working -> {
                        assertThat(working.getState()).isEqualTo(ContentItemState.WORKING);
                        assertThat(working.getData()).containsEntry("field1", "original");
                    });
            }
        }
    }

    @Nested
    @DisplayName("`DELETE /{identifier}` endpoint")
    class DeleteEndPoint {

        @Nested
        @DisplayName("When deleting existing content")
        class DeleteExistingContent {

            UUID identifier;

            @BeforeAll()
            void beforeAll() {
                mockContentItemIndexer();
                identifier = createDraft("SomeType", Map.of());
                contentItemEndpointClient.delete().uri("/{identifier}", identifier).exchange()
                    .expectStatus().isNoContent();
            }

            @Test
            @DisplayName("It should remove all versions of the content")
            void should_have_deleted_the_content() {
                contentItemEndpointClient.get().uri("/{identifier}", identifier).exchange()
                    .expectStatus().isNotFound();
            }
        }

        @Nested
        @DisplayName("When deleting an unknown identifier")
        class DeleteUnknownContent {

            @Test
            @DisplayName("It should return a `204 NO CONTENT` status code")
            void should_return_a_204() {
                mockContentItemIndexer();
                contentItemEndpointClient.delete().uri("/{identifier}", UuidV7.generate()).exchange()
                    .expectStatus().isNoContent();
            }
        }
    }
}
