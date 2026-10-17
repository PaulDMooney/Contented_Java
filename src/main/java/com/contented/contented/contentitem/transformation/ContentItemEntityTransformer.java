package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.model.ContentItemVersionEntity;

import java.util.function.Predicate;

public interface ContentItemEntityTransformer extends Transformer<ContentItemVersionEntity, ContentItemVersionEntity>, Predicate<ContentItemVersionEntity> {
}
