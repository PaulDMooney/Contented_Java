package com.contented.contented.contentlet.transformation;

import com.contented.contented.contentlet.ContentletEntity;

import java.util.function.Predicate;

public interface Transformer<T, R> extends Predicate<T> {

    R transform(T toTransform);
}
