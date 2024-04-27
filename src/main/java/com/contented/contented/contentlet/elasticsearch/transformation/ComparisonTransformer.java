package com.contented.contented.contentlet.elasticsearch.transformation;

import jakarta.annotation.Nullable;

public interface ComparisonTransformer<T, R> {

    R transform(T toTransform, @Nullable T toCompare);
}
