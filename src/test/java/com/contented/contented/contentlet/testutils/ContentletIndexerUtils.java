package com.contented.contented.contentlet.testutils;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.elasticsearch.ContentletIndexer;
import org.mockito.stubbing.OngoingStubbing;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ContentletIndexerUtils {

    public static OngoingStubbing<Mono<ContentletEntity>> passThroughContentletIndexer(ContentletIndexer toMock) {
        return when(toMock.indexContentlet(any()))
            .thenAnswer(invocation -> {
                if (invocation.getArgument(0) != null) {
                    return Mono.just(invocation.getArgument(0));
                } else {
                    return Mono.empty();
                }
            });
    }
}
