package com.contented.contented.contentlet.testutils;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.ContentletRepository;
import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class StubbingUtils {

    public static void passThrough_indexContentlet(ContentletIndexer toMock) {
        when(toMock.indexContentlet(any(), any())).thenReturn(Mono.empty());
    }

    public static void passThrough_deleteRecord(ContentletIndexer toMock) {
        when(toMock.deleteRecord(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    /**
     * Mocks `save` and `saveAll` methods to simply return the EntityMaps passed to them.
     * @param toMock
     */
    public static void passthroughElasticSearchOperations(ReactiveElasticsearchOperations toMock) {

        when(toMock.saveAll(any(Iterable.class), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));

        when(toMock.save(any(EntityAsMap.class), any()))
            .thenAnswer(invocation -> {
                if (invocation.getArgument(0) != null) {
                    return Mono.just(invocation.getArgument(0));
                } else {
                    return Mono.empty();
                }
            });

        when(toMock.delete(anyString(), any(IndexCoordinates.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    public static OngoingStubbing<Mono<ContentletEntity>> passthroughContentletRepository(ContentletRepository toMock) {
        return when(toMock.save(any(ContentletEntity.class)))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(0) != null) {
                        return Mono.just(invocation.getArgument(0));
                    } else {
                        return Mono.empty();
                    }
                });
    }
}
