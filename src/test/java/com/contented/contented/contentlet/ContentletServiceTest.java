package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import com.contented.contented.contentlet.elasticsearch.transformation.BlogTransformer;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.contented.contented.contentlet.transformation.StandardDMSContentTransformer;
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

import static com.contented.contented.contentlet.testutils.StubbingUtils.passthroughContentletRepository;
import static com.contented.contented.contentlet.testutils.StubbingUtils.passthroughElasticSearchOperations;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@DisplayName("ContentletService")
public class ContentletServiceTest {

    static ContentletService newServiceWith(ContentletRepository repository) {
        passthroughContentletRepository(repository);
        var elasticsearchOperations = Mockito.mock(ElasticsearchOperations.class);
        passthroughElasticSearchOperations(elasticsearchOperations);
        var contentletIndexer = new ContentletIndexer(elasticsearchOperations, mock(IndexCoordinates.class), List.of(new BlogTransformer()));
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        var transformationHandler = new TransformationHandler(List.of(new StandardDMSContentTransformer(clock)));
        return new ContentletService(repository, contentletIndexer, transformationHandler);
    }

    @NestedPerClass
    @DisplayName("create")
    class Create {

        ContentletRepository repository = Mockito.mock(ContentletRepository.class);
        ContentletService contentletService;

        // A contentType that matches no transformer keeps this a pass-through save.
        ContentletEntity toCreate = new ContentletEntity(null, "SomeType", Map.of());
        ContentletEntity created;

        @BeforeAll
        void beforeAll() {
            contentletService = newServiceWith(repository);

            // When
            created = contentletService.create(toCreate);
        }

        @Test
        @DisplayName("it should save the contentlet")
        void should_save_contentlet() {
            verify(repository, times(1)).save(toCreate);
        }

        @Test
        @DisplayName("it should assign a generated id")
        void should_assign_a_generated_id() {
            assertThat(created.getId()).isNotNull();
        }

        @Test
        @DisplayName("it should mark the contentlet as new")
        void should_mark_the_contentlet_as_new() {
            assertThat(created.isNew()).isTrue();
        }
    }

    @NestedPerClass
    @DisplayName("create given content that matches criteria for entity transformations")
    class CreateWithTransformations {

        ContentletRepository repository = Mockito.mock(ContentletRepository.class);
        ContentletService contentletService;

        ContentletEntity toCreate = new ContentletEntity(null, "Blog",
            Map.ofEntries(
                entry("language", "EN")));

        @BeforeAll
        void beforeAll() {
            contentletService = newServiceWith(repository);

            // When
            contentletService.create(toCreate);
        }

        @Test
        @DisplayName("it should apply transformations before saving")
        void it_should_apply_transformations_before_saving() {

            var argumentCaptor = ArgumentCaptor.forClass(ContentletEntity.class);
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
        @DisplayName("when the contentlet exists")
        class WhenContentletExists {

            ContentletRepository repository = Mockito.mock(ContentletRepository.class);
            ContentletService contentletService;

            UUID id = UuidV7.generate();
            ContentletEntity toUpdate = new ContentletEntity(id, "SomeType", Map.of());
            Optional<ContentletEntity> result;

            @BeforeAll
            void beforeAll() {
                contentletService = newServiceWith(repository);

                // Given an existing contentlet with the same (immutable) contentType
                when(repository.findById(id)).thenReturn(Optional.of(new ContentletEntity(id, "SomeType", Map.of())));

                // When
                result = contentletService.update(id, toUpdate);
            }

            @Test
            @DisplayName("it should save the contentlet")
            void should_save_contentlet() {
                verify(repository, times(1)).save(toUpdate);
            }

            @Test
            @DisplayName("it should return the updated contentlet")
            void should_return_the_updated_contentlet() {
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(id);
            }
        }

        @NestedPerClass
        @DisplayName("when the contentlet does not exist")
        class WhenContentletDoesNotExist {

            ContentletRepository repository = Mockito.mock(ContentletRepository.class);
            ContentletService contentletService;

            UUID id = UuidV7.generate();
            Optional<ContentletEntity> result;

            @BeforeAll
            void beforeAll() {
                contentletService = newServiceWith(repository);

                // Given the repository has no contentlet for this id (findById returns empty by default)

                // When
                result = contentletService.update(id, new ContentletEntity(id, "SomeType", Map.of()));
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

            ContentletRepository repository = Mockito.mock(ContentletRepository.class);
            ContentletService contentletService;

            UUID id = UuidV7.generate();
            Throwable thrown;

            @BeforeAll
            void beforeAll() {
                contentletService = newServiceWith(repository);

                // Given a stored contentlet whose contentType is "Blog"
                when(repository.findById(id)).thenReturn(Optional.of(new ContentletEntity(id, "Blog", Map.of())));

                // When an update supplies a different contentType
                thrown = catchThrowable(() -> contentletService.update(id, new ContentletEntity(id, "News", Map.of())));
            }

            @Test
            @DisplayName("it should reject the change as invalid")
            void should_reject_the_change() {
                assertThat(thrown).isInstanceOf(InvalidContentletException.class);
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

        ContentletRepository repository = Mockito.mock(ContentletRepository.class);
        ContentletService contentletService;

        Throwable thrown;

        @BeforeAll
        void beforeAll() {
            contentletService = newServiceWith(repository);

            // When creating without a contentType
            thrown = catchThrowable(() -> contentletService.create(new ContentletEntity(null, null, Map.of())));
        }

        @Test
        @DisplayName("it should reject the create as invalid")
        void should_reject_the_create() {
            assertThat(thrown).isInstanceOf(InvalidContentletException.class);
        }

        @Test
        @DisplayName("it should not save anything")
        void should_not_save_anything() {
            verify(repository, never()).save(any());
        }
    }
}
