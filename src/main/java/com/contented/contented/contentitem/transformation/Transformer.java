package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.model.ContentItemVersionEntity;

public interface Transformer<T, R> {

    R transform(T toTransform);
}
