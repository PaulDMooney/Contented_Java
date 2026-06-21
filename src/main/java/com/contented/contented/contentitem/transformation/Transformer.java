package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;

public interface Transformer<T, R> {

    R transform(T toTransform);
}
