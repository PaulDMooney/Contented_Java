package com.contented.contented.contentitem.testutils;

import com.contented.contented.contentitem.model.ContentItemVersionEntity;
import com.contented.contented.contentitem.ContentItemVersionRepository;
import com.contented.contented.contentitem.elasticsearch.ContentItemIndexer;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StubbingUtils {

    public static void passThrough_indexContentItem(ContentItemIndexer toMock) {
        when(toMock.indexContentItem(any())).thenReturn(Collections.emptyList());
    }

    public static void passThrough_deleteRecord(ContentItemIndexer toMock) {
        when(toMock.deleteRecord(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Mocks `save` methods to simply return the EntityMaps passed to them.
     * @param toMock
     */
    public static void passthroughElasticSearchOperations(ElasticsearchOperations toMock) {

        when(toMock.save(any(Iterable.class), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(toMock.save(any(EntityAsMap.class), any(IndexCoordinates.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    public static OngoingStubbing<ContentItemVersionEntity> passthroughContentItemVersionRepository(ContentItemVersionRepository toMock) {
        return when(toMock.save(any(ContentItemVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
