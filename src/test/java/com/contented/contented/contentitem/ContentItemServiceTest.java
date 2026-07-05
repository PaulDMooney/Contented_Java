package com.contented.contented.contentitem;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.exceptions.InvalidContentItemException;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemMapper;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.model.ContentItemState;
import com.contented.contented.contentitem.transformation.StandardDMSContentTransformer;
import com.contented.contented.contentitem.transformation.TransformationHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.contented.contented.contentitem.testutils.StubbingUtils.passthroughContentItemRepository;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ContentItemService")
public class ContentItemServiceTest {

    static ContentItemService newServiceWith(ContentItemRepository repository) {
        passthroughContentItemRepository(repository);
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        var transformationHandler = new TransformationHandler(List.of(new StandardDMSContentTransformer(clock)));
        return new ContentItemService(repository, transformationHandler, new ContentItemMapper(),
            mock(ApplicationEventPublisher.class));
    }

    static ContentItemDTO dto(String contentType, Map<String, Object> fields) {
        return ContentItemDTO.builder().contentType(contentType).data(fields).build();
    }

    static ContentItemEntity version(UUID versionId, UUID identifier, String contentType, ContentItemState state) {
        return ContentItemEntity.newVersion(versionId, identifier, contentType, state, Instant.now(), Map.of());
    }

    @Nested
    @DisplayName("`create()`")
    class Create {

        @Nested
        @DisplayName("When the `contentType` matches no transformer")
        class WhenContentTypeMatchesNoTransformer {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            ContentItemResponseDTO created;

            @BeforeAll
            void when() {
                created = newServiceWith(repository).create(dto("SomeType", Map.of()));
            }

            @Test
            @DisplayName("It should pass the entity to `ContentItemRepository#save`")
            void should_save_content_item() {
                verify(repository, times(1)).save(any(ContentItemEntity.class));
            }

            @Test
            @DisplayName("It should create the version as a new WORKING draft with a generated version id and identifier")
            void should_create_a_working_draft() {
                var captor = ArgumentCaptor.forClass(ContentItemEntity.class);
                verify(repository).save(captor.capture());
                assertThat(captor.getValue().isNew()).isTrue();
                assertThat(created.getVersionId()).isNotNull();
                assertThat(created.getIdentifier()).isNotNull();
                assertThat(created.getState()).isEqualTo(ContentItemState.WORKING);
            }
        }

        @Nested
        @DisplayName("When the content matches a transformer's criteria")
        class WhenContentMatchesATransformer {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);

            @BeforeAll
            void when() {
                newServiceWith(repository).create(dto("Blog", Map.ofEntries(entry("language", "EN"))));
            }

            @Test
            @DisplayName("It should apply transformations before saving")
            void it_should_apply_transformations_before_saving() {
                var captor = ArgumentCaptor.forClass(ContentItemEntity.class);
                verify(repository).save(captor.capture());

                assertThat(captor.getValue().getSchemalessData())
                    .hasEntrySatisfying("language", value -> assertThat(value).isEqualTo("en"))
                    .containsKey("modDate");
            }
        }

        @Nested
        @DisplayName("When no `contentType` is supplied")
        class WhenNoContentType {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            Throwable thrown;

            @BeforeAll
            void when() {
                thrown = catchThrowable(() -> newServiceWith(repository).create(dto(null, Map.of())));
            }

            @Test
            @DisplayName("It should reject the create as invalid")
            void should_reject_the_create() {
                assertThat(thrown).isInstanceOf(InvalidContentItemException.class);
            }

            @Test
            @DisplayName("It should not call `ContentItemRepository#save`")
            void should_not_save_anything() {
                verify(repository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("`editWorking()`")
    class EditWorking {

        @Nested
        @DisplayName("Given the content already has a WORKING version")
        class GivenAWorkingVersionExists {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            UUID workingId = UuidV7.generate();
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING))
                    .thenReturn(Optional.of(version(workingId, identifier, "SomeType", ContentItemState.WORKING)));

                result = service.editWorking(identifier, dto("SomeType", Map.of("field1", "updatedValue")));
            }

            @Test
            @DisplayName("It should update the existing working version in place")
            void should_update_working_in_place() {
                var captor = ArgumentCaptor.forClass(ContentItemEntity.class);
                verify(repository).save(captor.capture());
                assertThat(captor.getValue().getVersionId()).isEqualTo(workingId);
                assertThat(captor.getValue().isNew()).isFalse();
                assertThat(captor.getValue().getState()).isEqualTo(ContentItemState.WORKING);
            }
        }

        @Nested
        @DisplayName("Given the content has only a LIVE version (no working)")
        class GivenOnlyALiveVersionExists {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            UUID liveId = UuidV7.generate();
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING))
                    .thenReturn(Optional.empty());
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.LIVE))
                    .thenReturn(Optional.of(version(liveId, identifier, "SomeType", ContentItemState.LIVE)));

                result = service.editWorking(identifier, dto("SomeType", Map.of("field1", "draft")));
            }

            @Test
            @DisplayName("It should create a new WORKING version")
            void should_create_a_new_working_version() {
                var captor = ArgumentCaptor.forClass(ContentItemEntity.class);
                verify(repository).save(captor.capture());
                assertThat(captor.getValue().isNew()).isTrue();
                assertThat(captor.getValue().getState()).isEqualTo(ContentItemState.WORKING);
                assertThat(captor.getValue().getVersionId()).isNotEqualTo(liveId);
                assertThat(captor.getValue().getIdentifier()).isEqualTo(identifier);
            }
        }

        @Nested
        @DisplayName("Given the identifier is unknown")
        class GivenUnknownIdentifier {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                result = service.editWorking(UuidV7.generate(), dto("SomeType", Map.of()));
            }

            @Test
            @DisplayName("It should return an empty result")
            void should_return_empty() {
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("It should not call `ContentItemRepository#save`")
            void should_not_save() {
                verify(repository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Given a stored content whose `contentType` is `Blog`")
        class GivenStoredContentTypeIsBlog {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            Throwable thrown;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING))
                    .thenReturn(Optional.of(version(UuidV7.generate(), identifier, "Blog", ContentItemState.WORKING)));

                thrown = catchThrowable(() -> service.editWorking(identifier, dto("News", Map.of())));
            }

            @Test
            @DisplayName("It should reject an edit that changes the `contentType`")
            void should_reject_the_change() {
                assertThat(thrown).isInstanceOf(InvalidContentItemException.class);
            }

            @Test
            @DisplayName("It should not call `ContentItemRepository#save`")
            void should_not_save() {
                verify(repository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("`publish()`")
    class Publish {

        @Nested
        @DisplayName("Given a WORKING version and a current LIVE version exist")
        class GivenWorkingAndLiveExist {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            UUID workingId = UuidV7.generate();
            UUID liveId = UuidV7.generate();
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING))
                    .thenReturn(Optional.of(version(workingId, identifier, "SomeType", ContentItemState.WORKING)));
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.LIVE))
                    .thenReturn(Optional.of(version(liveId, identifier, "SomeType", ContentItemState.LIVE)));

                result = service.publish(identifier);
            }

            @Test
            @DisplayName("It should archive the old live version and promote the working version to live")
            void should_archive_old_live_and_promote_working() {
                var captor = ArgumentCaptor.forClass(ContentItemEntity.class);
                verify(repository, times(2)).save(captor.capture());
                assertThat(captor.getAllValues())
                    .anySatisfy(saved -> {
                        assertThat(saved.getVersionId()).isEqualTo(liveId);
                        assertThat(saved.getState()).isEqualTo(ContentItemState.ARCHIVED);
                    })
                    .anySatisfy(saved -> {
                        assertThat(saved.getVersionId()).isEqualTo(workingId);
                        assertThat(saved.getState()).isEqualTo(ContentItemState.LIVE);
                    });
            }

            @Test
            @DisplayName("It should return the now-live version")
            void should_return_the_live_version() {
                assertThat(result).isPresent();
                assertThat(result.get().getVersionId()).isEqualTo(workingId);
                assertThat(result.get().getState()).isEqualTo(ContentItemState.LIVE);
            }
        }

        @Nested
        @DisplayName("Given a WORKING version exists but nothing is live yet")
        class GivenWorkingButNoLive {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            UUID workingId = UuidV7.generate();
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING))
                    .thenReturn(Optional.of(version(workingId, identifier, "SomeType", ContentItemState.WORKING)));
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.LIVE))
                    .thenReturn(Optional.empty());

                result = service.publish(identifier);
            }

            @Test
            @DisplayName("It should promote the working version to live")
            void should_promote_working() {
                assertThat(result).isPresent();
                assertThat(result.get().getState()).isEqualTo(ContentItemState.LIVE);
                verify(repository, times(1)).save(any(ContentItemEntity.class));
            }
        }

        @Nested
        @DisplayName("Given the identifier is unknown")
        class GivenUnknownIdentifier {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                var identifier = UuidV7.generate();
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING)).thenReturn(Optional.empty());
                Mockito.when(repository.existsByIdentifier(identifier)).thenReturn(false);

                result = service.publish(identifier);
            }

            @Test
            @DisplayName("It should return an empty result")
            void should_return_empty() {
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("Given the content exists but has no WORKING version to publish")
        class GivenNothingToPublish {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            Throwable thrown;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING)).thenReturn(Optional.empty());
                Mockito.when(repository.existsByIdentifier(identifier)).thenReturn(true);

                thrown = catchThrowable(() -> service.publish(identifier));
            }

            @Test
            @DisplayName("It should reject the publish as invalid")
            void should_reject_publish() {
                assertThat(thrown).isInstanceOf(InvalidContentItemException.class);
            }
        }
    }

    @Nested
    @DisplayName("`restore()`")
    class Restore {

        @Nested
        @DisplayName("Given a version that belongs to the identifier and no working version exists")
        class GivenVersionBelongsAndNoWorking {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            UUID identifier = UuidV7.generate();
            UUID archivedId = UuidV7.generate();
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                var archived = ContentItemEntity.newVersion(archivedId, identifier, "SomeType",
                    ContentItemState.ARCHIVED, Instant.now(), Map.of("field1", "historic"));
                Mockito.when(repository.findById(archivedId)).thenReturn(Optional.of(archived));
                Mockito.when(repository.findByIdentifierAndState(identifier, ContentItemState.WORKING)).thenReturn(Optional.empty());

                result = service.restore(identifier, archivedId);
            }

            @Test
            @DisplayName("It should create a new WORKING version copying the source content")
            void should_create_working_from_source() {
                var captor = ArgumentCaptor.forClass(ContentItemEntity.class);
                verify(repository).save(captor.capture());
                assertThat(captor.getValue().isNew()).isTrue();
                assertThat(captor.getValue().getState()).isEqualTo(ContentItemState.WORKING);
                assertThat((String) captor.getValue().get("field1")).isEqualTo("historic");
            }
        }

        @Nested
        @DisplayName("Given the version is unknown")
        class GivenUnknownVersion {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void when() {
                var service = newServiceWith(repository);
                result = service.restore(UuidV7.generate(), UuidV7.generate());
            }

            @Test
            @DisplayName("It should return an empty result")
            void should_return_empty() {
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("It should not call `ContentItemRepository#save`")
            void should_not_save() {
                verify(repository, never()).save(any());
            }
        }
    }
}
