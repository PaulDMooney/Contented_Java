package com.contented.contented.contentlet.elasticsearch.transformation;

public interface CrudContainer<T, S> {

    T toSave();

    S toDelete();
}
