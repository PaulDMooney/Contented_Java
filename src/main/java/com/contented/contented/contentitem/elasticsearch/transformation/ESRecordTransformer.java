package com.contented.contented.contentitem.elasticsearch.transformation;

import com.contented.contented.contentitem.ContentItemEntity;
import com.contented.contented.contentitem.transformation.Transformer;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.util.Collection;
import java.util.function.Predicate;

public interface ESRecordTransformer extends Transformer<ContentItemEntity, Collection<EntityAsMap>>, Predicate<ContentItemEntity> {
}
