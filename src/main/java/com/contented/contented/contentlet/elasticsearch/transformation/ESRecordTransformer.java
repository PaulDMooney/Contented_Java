package com.contented.contented.contentlet.elasticsearch.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.transformation.Transformer;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.util.Collection;
import java.util.function.Predicate;

public interface ESRecordTransformer extends Transformer<ContentletEntity, Collection<EntityAsMap>>, Predicate<ContentletEntity> {
}
