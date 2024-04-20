package com.contented.contented.contentlet.testutils;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.ContentletRepository;
import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StubbingUtils {

    public static OngoingStubbing<Mono<EntityAsMap>> passThrough_indexContentlet(ContentletIndexer toMock) {
        return when(toMock.indexContentlet(any())).thenReturn(Mono.empty());
    }

    public static OngoingStubbing<Mono<String>> passThrough_deleteRecord(ContentletIndexer toMock) {
        return when(toMock.deleteRecord(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    public static OngoingStubbing<Mono<EntityAsMap>> passthroughElasticSearchOperations(ReactiveElasticsearchOperations toMock) {
        return when(toMock.save(any(EntityAsMap.class), any()))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(0) != null) {
                        return Mono.just(invocation.getArgument(0));
                    } else {
                        return Mono.empty();
                    }
                });
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
