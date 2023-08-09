package com.contented.contented.contentlet;

import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import com.contented.contented.contentlet.elasticsearch.transformation.BlogTransformer;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.contented.contented.contentlet.transformation.StandardDMSContentTransformer;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static com.contented.contented.contentlet.testutils.StubbingUtils.passthroughContentletRepository;
import static com.contented.contented.contentlet.testutils.StubbingUtils.passthroughElasticSearchOperations;
import static java.util.Map.entry;
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

        ReactiveElasticsearchOperations reactiveElasticsearchOperations;

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
            passthroughContentletRepository(repository);
            reactiveElasticsearchOperations = Mockito.mock(ReactiveElasticsearchOperations.class);
            passthroughElasticSearchOperations(reactiveElasticsearchOperations);
            contentletIndexer = new ContentletIndexer(reactiveElasticsearchOperations, null, List.of(new BlogTransformer()));
            Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
            var transformationHandler = new TransformationHandler(List.of(new StandardDMSContentTransformer(clock)));
            contentletService = new ContentletService(repository, contentletIndexer, transformationHandler);
        }

        @NestedPerClass
        @DisplayName("when saving contentlet that does not exist")
        class SavingContentletThatDoesNotExist {

            ContentletService.ResultPair result;

            @BeforeAll
            void beforeAll() {
                passthroughContentletRepository(repository); // because we reset the repository in afterAll

                // Given
                when(repository.existsById(Mockito.anyString())).thenReturn(Mono.just(false));

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

                passthroughContentletRepository(repository); // because we reset the repository in afterAll

                // Given
                when(repository.existsById(Mockito.anyString())).thenReturn(Mono.just(true));

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

        @NestedPerClass
        @DisplayName("Given content that matches criteria for entity transformations")
        class SavingContentletWithTransformations {

            ContentletEntity toSave = new ContentletEntity("1234",
                Map.ofEntries(
                    entry("language", "en"),
                    entry("stName", "Blog"),
                    entry("parentDmsId", "parentDmsIdABCDE")));

            @BeforeAll
            void beforeAll() {

                when(repository.existsById(Mockito.anyString())).thenReturn(Mono.just(false));
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

                    // Some expected Transformations
                    assertThat(savedValue.getSchemalessData())
                        .hasEntrySatisfying("contentType", value -> assertThat(value).isEqualTo("Blog"));
                    assertThat(savedValue.getSchemalessData())
                        .hasEntrySatisfying("identifier", value -> assertThat(value).isEqualTo("parentDmsIdABCDE"));

                }
            }
        }
    }
}
