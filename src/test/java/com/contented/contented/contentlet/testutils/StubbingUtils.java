package com.contented.contented.contentlet.testutils;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.ContentletRepository;
import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StubbingUtils {

    public static void passThrough_indexContentlet(ContentletIndexer toMock) {
        when(toMock.indexContentlet(any())).thenReturn(Collections.emptyList());
    }

    public static void passThrough_deleteRecord(ContentletIndexer toMock) {
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

    public static OngoingStubbing<ContentletEntity> passthroughContentletRepository(ContentletRepository toMock) {
        return when(toMock.save(any(ContentletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
