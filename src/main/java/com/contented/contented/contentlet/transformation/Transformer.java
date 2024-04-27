package com.contented.contented.contentlet.transformation;

public interface Transformer<T, R> {

    R transform(T toTransform);
}
