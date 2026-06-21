package com.contented.contented.contentitem.elasticsearch;

import com.contented.contented.contentitem.ContentItemEntity;
import com.contented.contented.contentitem.UuidV7;
import com.contented.contented.contentitem.elasticsearch.transformation.ESRecordTransformer;
import com.contented.contented.contentitem.testutils.NestedPerClass;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.contented.contented.contentitem.elasticsearch.transformation.StandardContentItemTransformations.applyStandardTransformations;
import static com.contented.contented.contentitem.testutils.StubbingUtils.passthroughElasticSearchOperations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@DisplayName("ContentItemIndexer")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentItemIndexerTest {

    ContentItemIndexer contentItemIndexer;

    ElasticsearchOperations elasticsearchOperations;

    @BeforeAll
    void beforeAll() {
        elasticsearchOperations = Mockito.mock(ElasticsearchOperations.class);
        passthroughElasticSearchOperations(elasticsearchOperations);
        contentItemIndexer = new ContentItemIndexer(elasticsearchOperations, Mockito.mock(IndexCoordinates.class),
            List.of(new MultiEntityContentItemTransformer()));
    }

    @DisplayName("indexContentItem")
    @NestedPerClass
    class IndexContentItem {

        @DisplayName("Given a contentItem whose transformer spawns multiple ElasticSearch Entities")
        @NestedPerClass
        class GivenContentItemWhoseTransformerSpawnsMultipleEntities {

            ContentItemEntity contentItemEntity = new ContentItemEntity(UuidV7.generate(), "TestMultiEntityContentType",
                Map.of("identifier", "identifier1"));

            @Test
            @DisplayName("it should pass each entity to the underlying ElasticsearchOperations#save to save them all")
            void shouldPassEachEntityToTheUnderlyingElasticsearchOperationsSaveToSaveThemAll() {

                var saveAllArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);

                // When
                contentItemIndexer.indexContentItem(contentItemEntity);

                verify(elasticsearchOperations).save(saveAllArgumentCaptor.capture(), any(IndexCoordinates.class));

                var savedEntities = (Collection<EntityAsMap>) saveAllArgumentCaptor.getValue();

                assertThat(savedEntities).isNotNull();

                // Size we expect from MultiEntityContentItemTransformer
                assertThat(savedEntities).hasSize(3);
                assertThat(savedEntities).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_1"),"generated identifier1_1"));
                assertThat(savedEntities).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_2"),"generated identifier1_2"));
                assertThat(savedEntities).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_3"),"generated identifier1_3"));
            }

            @Test
            @DisplayName("it should return a list of the entities that were saved")
            void shouldReturnAListOfTheEntitiesThatWereSaved() {
                // When
                var result = contentItemIndexer.indexContentItem(contentItemEntity);

                // Then
                assertThat(result).hasSize(3);
                assertThat(result).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_1"),"generated identifier1_1"));
                assertThat(result).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_2"),"generated identifier1_2"));
                assertThat(result).haveExactly(1, new Condition<>(entityAsMap -> entityAsMap.get("identifier").equals("identifier1_3"),"generated identifier1_3"));
            }

        }

        @DisplayName("Given a contentItem with no matching transformer")
        @NestedPerClass
        class GivenContentItemWithNoMatchingTransformer {

            ContentItemEntity contentItemEntity = new ContentItemEntity(UuidV7.generate(), "TypeWithNoTransformer",
                Map.of("identifier", "identifier1"));

            @Test
            @DisplayName("it should return an empty list")
            void shouldReturnAnEmptyList() {
                // When
                var result = contentItemIndexer.indexContentItem(contentItemEntity);

                // Then
                assertThat(result).isEmpty();
            }
        }
    }

    static class MultiEntityContentItemTransformer implements ESRecordTransformer {
        @Override
        public Collection<EntityAsMap> transform(ContentItemEntity toTransform) {
            EntityAsMap entityAsMap1 = new EntityAsMap();
            entityAsMap1.put("identifier", toTransform.get("identifier") + "_1");

            EntityAsMap entityAsMap2 = new EntityAsMap();
            entityAsMap2.put("identifier", toTransform.get("identifier") + "_2");

            EntityAsMap entityAsMap3 = new EntityAsMap();
            entityAsMap3.put("identifier", toTransform.get("identifier") + "_3");

            return List.of(entityAsMap1, entityAsMap2, entityAsMap3);
        }

        @Override
        public boolean test(ContentItemEntity contentItemEntity) {
            return "TestMultiEntityContentType".equals(contentItemEntity.getContentType());
        }
    }
}