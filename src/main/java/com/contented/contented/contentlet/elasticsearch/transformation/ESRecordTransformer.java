package com.contented.contented.contentlet.elasticsearch.transformation;

import com.contented.contented.contentlet.ContentletEntity;

import java.util.function.Predicate;

public interface ESRecordTransformer extends ComparisonTransformer<ContentletEntity, ESCrudContainer>, Predicate<ContentletEntity> {

}
