package com.contented.contented.contentlet.elasticsearch;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.elasticsearch.transformation.ESRecordTransformer;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.contented.contented.contentlet.elasticsearch.transformation.StandardContentletTransformations.applyStandardTransformations;
import static com.contented.contented.contentlet.testutils.StubbingUtils.passthroughElasticSearchOperations;
import static com.contented.contented.contentlet.transformation.StandardDMSContentTransformer.CONTENT_TYPE_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@DisplayName("ContentletIndexer")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentletIndexerTest {

    ContentletIndexer contentletIndexer;

    ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    @BeforeAll
    void beforeAll() {
        reactiveElasticsearchOperations = Mockito.mock(ReactiveElasticsearchOperations.class);
        passthroughElasticSearchOperations(reactiveElasticsearchOperations);
        contentletIndexer = new ContentletIndexer(reactiveElasticsearchOperations, Mockito.mock(IndexCoordinates.class),
            List.of(new MultiEntityContentletTransformer()));
    }

    @DisplayName("indexContentlet")
    @NestedPerClass
    class IndexContentlet {

        @DisplayName("Given a contentlet whose transformer spawns multiple ElasticSearch Entities")
        @NestedPerClass
        class GivenContentletWhoseTransformerSpawnsMultipleEntities {

            ContentletEntity contentletEntity = new ContentletEntity("Contentlet1",
                Map.of("identifier", "identifier1",
                    CONTENT_TYPE_FIELD, "TestMultiEntityContentType"));

            @Test
            @DisplayName("it should pass each entity to the underlying ReactiveElasticsearchOperations#savall to save them all")
            void shouldPassEachEntityToTheUnderlyingReactiveElasticsearchOperationsSaveAllToSaveThemAll() {

                var saveAllArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);

                // When
                contentletIndexer.indexContentlet(contentletEntity, null).block();

                verify(reactiveElasticsearchOperations).saveAll(saveAllArgumentCaptor.capture(), any(IndexCoordinates.class));

                var savedEntities = (Collection<EntityAsMap>) saveAllArgumentCaptor.getValue();

                assertThat(savedEntities).isNotNull();

                // Size we expect from MultiEntityContentletTransformer
                assertThat(savedEntities).hasSize(3);
                assertThat(savedEntities).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_1"),"generated identifier1_1"));
                assertThat(savedEntities).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_2"),"generated identifier1_2"));
                assertThat(savedEntities).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_3"),"generated identifier1_3"));
            }

            @Test
            @DisplayName("it should return a list of the entities that were saved")
            void shouldReturnAListOfTheEntitiesThatWereSaved() {
                // When
                var result = contentletIndexer.indexContentlet(contentletEntity, null).block();

                // Then
                assertThat(result).hasSize(3);
                assertThat(result).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_1"),"generated identifier1_1"));
                assertThat(result).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_2"),"generated identifier1_2"));
                assertThat(result).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_3"),"generated identifier1_3"));
            }

        }

        @DisplayName("Given a contentlet with no matching transformer")
        @NestedPerClass
        class GivenContentletWithNoMatchingTransformer {

            ContentletEntity contentletEntity = new ContentletEntity("Contentlet1",
                Map.of("identifier", "identifier1",
                    CONTENT_TYPE_FIELD, "TypeWithNoTransformer"));

            @Test
            @DisplayName("it should return an empty mono")
            void shouldReturnAnEmptyMono() {
                // When
                var result = contentletIndexer.indexContentlet(contentletEntity, null);

                // Then
                StepVerifier.create(result)
                    .expectComplete()
                    .verify();
            }
        }
    }

    static class MultiEntityContentletTransformer implements ESRecordTransformer {
        @Override
        public Collection<EntityAsMap> transform(ContentletEntity toTransform) {
            EntityAsMap entityAsMap1 = new EntityAsMap();
            entityAsMap1.put("identifier", toTransform.get("identifier") + "_1");

            EntityAsMap entityAsMap2 = new EntityAsMap();
            entityAsMap2.put("identifier", toTransform.get("identifier") + "_2");

            EntityAsMap entityAsMap3 = new EntityAsMap();
            entityAsMap3.put("identifier", toTransform.get("identifier") + "_3");

            return List.of(entityAsMap1, entityAsMap2, entityAsMap3);
        }

        @Override
        public boolean test(ContentletEntity contentletEntity) {
            return "TestMultiEntityContentType".equals(contentletEntity.getSchemalessData().get(CONTENT_TYPE_FIELD));
        }
    }
}