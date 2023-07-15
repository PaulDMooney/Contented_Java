package com.contented.contented.contentlet.transformation;

import com.contented.contented.contentlet.ContentletEntity;

public interface Transformer<T, R> {

    R transform(T toTransform);
}
