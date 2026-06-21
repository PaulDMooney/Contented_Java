package com.contented.contented.contentitem;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import com.contented.contented.contentitem.elasticsearch.transformation.BlogTransformer;
import com.contented.contented.contentitem.exceptions.InvalidContentItemException;
import com.contented.contented.contentitem.testutils.NestedPerClass;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemMapper;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.transformation.StandardDMSContentTransformer;
import com.contented.contented.contentitem.transformation.TransformationHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.contented.contented.contentitem.testutils.StubbingUtils.passthroughContentItemRepository;
import static com.contented.contented.contentitem.testutils.StubbingUtils.passthroughElasticSearchOperations;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@DisplayName("ContentItemService")
public class ContentItemServiceTest {

    static ContentItemService newServiceWith(ContentItemRepository repository) {
        passthroughContentItemRepository(repository);
        var elasticsearchOperations = Mockito.mock(ElasticsearchOperations.class);
        passthroughElasticSearchOperations(elasticsearchOperations);
        var contentItemIndexer = new ContentItemIndexer(elasticsearchOperations, mock(IndexCoordinates.class), List.of(new BlogTransformer()));
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        var transformationHandler = new TransformationHandler(List.of(new StandardDMSContentTransformer(clock)));
        return new ContentItemService(repository, contentItemIndexer, transformationHandler, new ContentItemMapper());
    }

    static ContentItemDTO dto(UUID id, String contentType, Map<String, Object> fields) {
        var dto = new ContentItemDTO(id);
        dto.setContentType(contentType);
        fields.forEach(dto::add);
        return dto;
    }

    @NestedPerClass
    @DisplayName("create")
    class Create {

        ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
        ContentItemService contentItemService;

        // A contentType that matches no transformer keeps this a pass-through save.
        ContentItemDTO toCreate = dto(null, "SomeType", Map.of());
        ContentItemResponseDTO created;

        @BeforeAll
        void beforeAll() {
            contentItemService = newServiceWith(repository);

            // When
            created = contentItemService.create(toCreate);
        }

        @Test
        @DisplayName("it should save the contentItem")
        void should_save_content_item() {
            verify(repository, times(1)).save(any(ContentItemEntity.class));
        }

        @Test
        @DisplayName("it should assign a generated id")
        void should_assign_a_generated_id() {
            assertThat(created.getId()).isNotNull();
        }

        @Test
        @DisplayName("it should mark the saved contentItem as new")
        void should_mark_the_content_item_as_new() {
            var argumentCaptor = ArgumentCaptor.forClass(ContentItemEntity.class);
            verify(repository).save(argumentCaptor.capture());

            assertThat(argumentCaptor.getValue().isNew()).isTrue();
        }
    }

    @NestedPerClass
    @DisplayName("create given content that matches criteria for entity transformations")
    class CreateWithTransformations {

        ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
        ContentItemService contentItemService;

        ContentItemDTO toCreate = dto(null, "Blog", Map.ofEntries(entry("language", "EN")));

        @BeforeAll
        void beforeAll() {
            contentItemService = newServiceWith(repository);

            // When
            contentItemService.create(toCreate);
        }

        @Test
        @DisplayName("it should apply transformations before saving")
        void it_should_apply_transformations_before_saving() {

            var argumentCaptor = ArgumentCaptor.forClass(ContentItemEntity.class);
            verify(repository).save(argumentCaptor.capture());

            var savedValue = argumentCaptor.getValue();

            // Some expected Transformations
            assertThat(savedValue.getSchemalessData())
                .hasEntrySatisfying("language", value -> assertThat(value).isEqualTo("en"));
            assertThat(savedValue.getSchemalessData())
                .containsKey("modDate");
        }
    }

    @NestedPerClass
    @DisplayName("update")
    class Update {

        @NestedPerClass
        @DisplayName("when the contentItem exists")
        class WhenContentItemExists {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            ContentItemService contentItemService;

            UUID id = UuidV7.generate();
            ContentItemDTO toUpdate = dto(id, "SomeType", Map.of());
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void beforeAll() {
                contentItemService = newServiceWith(repository);

                // Given an existing contentItem with the same (immutable) contentType
                when(repository.findById(id)).thenReturn(Optional.of(new ContentItemEntity(id, "SomeType", Map.of())));

                // When
                result = contentItemService.update(id, toUpdate);
            }

            @Test
            @DisplayName("it should save the contentItem")
            void should_save_content_item() {
                verify(repository, times(1)).save(any(ContentItemEntity.class));
            }

            @Test
            @DisplayName("it should return the updated contentItem")
            void should_return_the_updated_content_item() {
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(id);
            }
        }

        @NestedPerClass
        @DisplayName("when the contentItem does not exist")
        class WhenContentItemDoesNotExist {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            ContentItemService contentItemService;

            UUID id = UuidV7.generate();
            Optional<ContentItemResponseDTO> result;

            @BeforeAll
            void beforeAll() {
                contentItemService = newServiceWith(repository);

                // Given the repository has no contentItem for this id (findById returns empty by default)

                // When
                result = contentItemService.update(id, dto(id, "SomeType", Map.of()));
            }

            @Test
            @DisplayName("it should return an empty result")
            void should_return_empty_result() {
                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("it should not save anything")
            void should_not_save_anything() {
                verify(repository, never()).save(any());
            }
        }

        @NestedPerClass
        @DisplayName("when the contentType differs from the stored one")
        class WhenContentTypeChanges {

            ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
            ContentItemService contentItemService;

            UUID id = UuidV7.generate();
            Throwable thrown;

            @BeforeAll
            void beforeAll() {
                contentItemService = newServiceWith(repository);

                // Given a stored contentItem whose contentType is "Blog"
                when(repository.findById(id)).thenReturn(Optional.of(new ContentItemEntity(id, "Blog", Map.of())));

                // When an update supplies a different contentType
                thrown = catchThrowable(() -> contentItemService.update(id, dto(id, "News", Map.of())));
            }

            @Test
            @DisplayName("it should reject the change as invalid")
            void should_reject_the_change() {
                assertThat(thrown).isInstanceOf(InvalidContentItemException.class);
            }

            @Test
            @DisplayName("it should not save anything")
            void should_not_save_anything() {
                verify(repository, never()).save(any());
            }
        }
    }

    @NestedPerClass
    @DisplayName("when the contentType is missing")
    class WhenContentTypeMissing {

        ContentItemRepository repository = Mockito.mock(ContentItemRepository.class);
        ContentItemService contentItemService;

        Throwable thrown;

        @BeforeAll
        void beforeAll() {
            contentItemService = newServiceWith(repository);

            // When creating without a contentType
            thrown = catchThrowable(() -> contentItemService.create(dto(null, null, Map.of())));
        }

        @Test
        @DisplayName("it should reject the create as invalid")
        void should_reject_the_create() {
            assertThat(thrown).isInstanceOf(InvalidContentItemException.class);
        }

        @Test
        @DisplayName("it should not save anything")
        void should_not_save_anything() {
            verify(repository, never()).save(any());
        }
    }
}
