package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static com.contented.contented.contentlet.testutils.ContentletIndexerUtils.passThroughContentletIndexer;
import static org.mockito.Mockito.*;

public class ContentletServiceTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
            Assertions.assertThat(saved.getId()).isEqualTo(toSave.getId());
        }

        @BeforeAll
        void beforeAll() {
            repository = Mockito.mock(ContentletRepository.class);
            contentletIndexer = Mockito.mock(ContentletIndexer.class);
            passThroughContentletIndexer(contentletIndexer);
            contentletService = new ContentletService(repository, contentletIndexer);
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                Assertions.assertThat(result.isNew()).isTrue();
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                Assertions.assertThat(result.isNew()).isFalse();
            }
        }
    }
}
