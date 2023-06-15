package com.contented.contented.contentlet;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentletDTO {

    private String id;

    @JsonIgnore
    private Map<String, Object> schemalessData = new LinkedHashMap<>();

    public ContentletDTO(String id) {
        this(id, new LinkedHashMap<>());
    }
    @JsonAnySetter
    public void add(String key, Object value) {
        schemalessData.put(key, value);
    }

    @JsonAnyGetter
    public Map<String,Object> get() {
        return schemalessData;
    }
}
