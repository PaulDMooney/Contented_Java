package com.contented.contented.contentitem.model;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public abstract class AbstractContentItemDTO {

    private String contentType;

    // The schemaless content, nested so it cannot collide with or override the fixed/system fields.
    private Map<String, Object> data = new LinkedHashMap<>();
}
