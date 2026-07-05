package com.contented.contented.contentitem.model;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@SuperBuilder
public abstract class AbstractContentItemDTO {

    private final String contentType;

    // The schemaless content, nested so it cannot collide with or override the fixed/system fields.
    @Builder.Default
    private final Map<String, Object> data = new LinkedHashMap<>();
}
