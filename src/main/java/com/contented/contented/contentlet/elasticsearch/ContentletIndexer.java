package com.contented.contented.contentlet.elasticsearch;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.elasticsearch.transformation.ESCrudContainer;
import com.contented.contented.contentlet.elasticsearch.transformation.ESRecordTransformer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Component
public class ContentletIndexer {

    final ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    final IndexCoordinates indexCoordinates;

    final List<ESRecordTransformer> esTransformers;

    @Autowired
    public ContentletIndexer(ReactiveElasticsearchOperations reactiveElasticsearchOperations,
                             IndexCoordinates indexCoordinates,
                             List<ESRecordTransformer> esTransformers) {
        this.reactiveElasticsearchOperations = reactiveElasticsearchOperations;
        this.indexCoordinates = indexCoordinates;
        this.esTransformers = esTransformers;
    }

    public Mono<List<EntityAsMap>> indexContentlet(@Nonnull final ContentletEntity contentletEntity,
                                                   @Nullable final ContentletEntity previousContentletEntityVersion) {
        return esTransformers.stream()
                .filter(esRecordTransformer -> esRecordTransformer.test(contentletEntity))
                .findFirst()
                .map(esRecordTransformer -> {
                    var esCrudContainer = esRecordTransformer.transform(contentletEntity, previousContentletEntityVersion);

                    return saveAndDeleteESEntities(esCrudContainer);
                })
                .orElseGet(() -> {
                    log.warn("No transformer found for contentlet: {}", contentletEntity.getId());
                    return Mono.empty();
                });
    }

    private Mono<List<EntityAsMap>> saveAndDeleteESEntities(ESCrudContainer esCrudContainer) {
        final Mono<List<EntityAsMap>> saveMono;

        if (!CollectionUtils.isEmpty(esCrudContainer.toSave())) {
            saveMono = reactiveElasticsearchOperations.saveAll(esCrudContainer.toSave(), indexCoordinates)
                .collectList();
        } else {
            saveMono = Mono.just(Collections.EMPTY_LIST);
        }

        final Mono<List<String>> deleteMono;
        if (!CollectionUtils.isEmpty(esCrudContainer.toDelete())) {
            deleteMono = deleteAll(esCrudContainer.toDelete());
        } else {
            deleteMono = Mono.just(Collections.EMPTY_LIST);
        }

        return Mono.zip(saveMono, deleteMono, (savedEntities, deletedIds) -> {
            log.info("Saved {} entities and deleted {} entities", savedEntities.size(), deletedIds.size());
            return savedEntities;
        });
    }

    private Mono<List<String>> deleteAll(Collection<String> idsToDelete) {

        var deleteOperations = idsToDelete.stream()
            .map(id -> reactiveElasticsearchOperations.delete(id, indexCoordinates))
            .collect(Collectors.toList());

        return Mono.zip(deleteOperations, returnedIds -> Arrays.stream(returnedIds)
            .map(Object::toString)
            .collect(Collectors.toList()));
    }

    public Mono<String> deleteRecord(String id) {
        return reactiveElasticsearchOperations.delete(id, indexCoordinates);
    }
}
