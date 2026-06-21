package com.contented.contented.contentitem.elasticsearch;

import com.contented.contented.contentitem.ContentItemEntity;
import com.contented.contented.contentitem.elasticsearch.transformation.ESRecordTransformer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Component
public class ContentItemIndexer {

    final ElasticsearchOperations elasticsearchOperations;

    final IndexCoordinates indexCoordinates;

    final List<ESRecordTransformer> esTransformers;

    @Autowired
    public ContentItemIndexer(ElasticsearchOperations elasticsearchOperations,
                             IndexCoordinates indexCoordinates,
                             List<ESRecordTransformer> esTransformers) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.indexCoordinates = indexCoordinates;
        this.esTransformers = esTransformers;
    }

    public List<EntityAsMap> indexContentItem(ContentItemEntity contentItemEntity) {
        return esTransformers.stream()
                .filter(esRecordTransformer -> esRecordTransformer.test(contentItemEntity))
                .findFirst()
                .map(esRecordTransformer -> {
                    var transformedEntities = esRecordTransformer.transform(contentItemEntity);
                    var savedEntities = elasticsearchOperations.save(transformedEntities, indexCoordinates);
                    List<EntityAsMap> savedEntitiesList = new ArrayList<>();
                    savedEntities.forEach(savedEntitiesList::add);
                    return savedEntitiesList;
                })
                .orElseGet(() -> {
                    log.warn("No transformer found for contentItem: {}", contentItemEntity.getId());
                    return Collections.emptyList();
                });
    }

    public String deleteRecord(String id) {
        return elasticsearchOperations.delete(id, indexCoordinates);
    }
}
