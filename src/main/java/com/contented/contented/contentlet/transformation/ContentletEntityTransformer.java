package com.contented.contented.contentlet.transformation;

import com.contented.contented.contentlet.ContentletEntity;

import java.util.function.Predicate;

public interface ContentletEntityTransformer extends Transformer<ContentletEntity, ContentletEntity>, Predicate<ContentletEntity> {
}
