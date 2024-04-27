package com.contented.contented.contentlet.elasticsearch.transformation;

import jakarta.annotation.Nullable;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.util.Collection;

public record ESCrudContainer(@Nullable Collection<EntityAsMap> toSave, @Nullable Collection<String> toDelete) implements CrudContainer<Collection<EntityAsMap>, Collection<String>>{
}
