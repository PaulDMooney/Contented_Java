package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.contented.contented.contentlet.testutils.ContentletIndexerUtils.passThroughContentletIndexer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ContentletService")
public class ContentletServiceTest {

    @NestedPerClass
    @DisplayName("save")
    class Save {

        ContentletService contentletService;
        ContentletRepository repository;

        ContentletIndexer contentletIndexer;

        ContentletEntity toSave = new ContentletEntity("Contentlet1");

        void verifyContentletSaved() {

            // Could be a better test if we didn't have to know what ContentletRepository calls were being made.
            // Then we could change out the calls and know the behavior was still correct.
            verify(repository,times(1)).save(toSave);
        }

        void assertSameContentletSaved(ContentletEntity saved) {
            assertThat(saved.getId()).isEqualTo(toSave.getId());
        }

        @BeforeAll
        void beforeAll() {
            repository = Mockito.mock(ContentletRepository.class);
            contentletIndexer = Mockito.mock(ContentletIndexer.class);
            passThroughContentletIndexer(contentletIndexer);
            contentletService = new ContentletService(repository, contentletIndexer);
        }

        @NestedPerClass
        @DisplayName("when saving contentlet that does not exist")
        class SavingContentletThatDoesNotExist {

            ContentletService.ResultPair result;

            @BeforeAll
            void beforeAll() {
                // Given
                when(repository.existsById(Mockito.anyString())).thenReturn(Mono.just(false));
                when(repository.save(Mockito.any(ContentletEntity.class))).thenReturn(Mono.just(toSave));

                // when
                result = contentletService.save(toSave).block();
            }

            @AfterAll
            void afterAll() {
                Mockito.reset(repository);
            }

            @Test
            @DisplayName("it should save contentlet")
            void should_save_contentlet() {
                verifyContentletSaved();
            }

            @Test
            @DisplayName("it should return result pair containing saved contentlet")
            void should_return_result_pair_with_saved_contentlet() {
                assertSameContentletSaved(result.contentletEntity());
            }

            @Test
            @DisplayName("it should return result pair with isNew=true")
            void should_return_result_pair_with_isNew_true() {
                assertThat(result.isNew()).isTrue();
            }
        }

        @NestedPerClass
        @DisplayName("when saving contentlet that does exist")
        class SavingContentletThatDoesExist {

            ContentletService.ResultPair result;
            @BeforeAll
            void beforeAll() {
                // Given
                when(repository.existsById(Mockito.anyString())).thenReturn(Mono.just(true));
                when(repository.save(Mockito.any(ContentletEntity.class))).thenReturn(Mono.just(toSave));

                result = contentletService.save(toSave).block();
            }

            @AfterAll
            void afterAll() {
                Mockito.reset(repository);
            }

            @Test
            @DisplayName("it should save contentlet")
            void should_save_contentlet() {
                verifyContentletSaved();
            }

            @Test
            @DisplayName("it should return result pair containing saved contentlet")
            void should_return_result_pair_with_saved_contentlet_and_isNew_false() {
                assertSameContentletSaved(result.contentletEntity());
            }

            @Test
            @DisplayName("it should return result pair with isNew=false")
            void should_return_result_pair_with_isNew_false() {
                assertThat(result.isNew()).isFalse();
            }
        }

        @Disabled
        @NestedPerClass
        @DisplayName("Given content that matches criteria for transformations")
        class SavingContentletWithTransformations {

            ContentletEntity toSave = new ContentletEntity("1234",
                Map.of("language", "en", "stName", "Blog"));

            @BeforeAll
            void beforeAll() {
                repository = Mockito.mock(ContentletRepository.class);
                contentletIndexer = Mockito.mock(ContentletIndexer.class);
                passThroughContentletIndexer(contentletIndexer);
                contentletService = new ContentletService(repository, contentletIndexer);

                when(repository.existsById(Mockito.anyString())).thenReturn(Mono.just(false));
                when(repository.save(Mockito.any(ContentletEntity.class))).thenReturn(Mono.just(toSave));
            }

            @NestedPerClass
            @DisplayName("when saving contentlet")
            class WhenSavingContentlet {

                @BeforeAll
                void beforeAll() {
                    contentletService.save(toSave).block();
                }

                @Test()
                @DisplayName("it should apply transformations before saving")
                void it_should_apply_transformations_before_saving() {

                    var argumentCaptor = ArgumentCaptor.forClass(ContentletEntity.class);
                    verify(repository).save(argumentCaptor.capture());

                    var savedValue = argumentCaptor.getValue();
                    assertThat(savedValue.getSchemalessData())
                        .hasEntrySatisfying("contentType", value -> assertThat(value).isEqualTo("Blog"));
                    assertThat(savedValue.getSchemalessData())
                        .hasEntrySatisfying("identifier", value -> assertThat(value).isEqualTo("1234_en"));

                }
            }

        }
    }
}
