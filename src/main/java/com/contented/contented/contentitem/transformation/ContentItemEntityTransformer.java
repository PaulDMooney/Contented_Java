package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.ContentItemEntity;

import java.util.function.Predicate;

public interface ContentItemEntityTransformer extends Transformer<ContentItemEntity, ContentItemEntity>, Predicate<ContentItemEntity> {
}
