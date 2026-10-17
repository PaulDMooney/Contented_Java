package com.contented.contented.contentitem.elasticsearch.transformation;

import com.contented.contented.contentitem.model.ContentItemVersionEntity;
import com.contented.contented.contentitem.transformation.Transformer;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.util.Collection;
import java.util.function.Predicate;

public interface ESRecordTransformer extends Transformer<ContentItemVersionEntity, Collection<EntityAsMap>>, Predicate<ContentItemVersionEntity> {
}
