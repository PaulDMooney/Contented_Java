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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentletDTO {

    private UUID id;

    private String contentType;

    @JsonIgnore
    private Map<String, Object> schemalessData = new LinkedHashMap<>();

    public ContentletDTO(UUID id) {
        this(id, null, new LinkedHashMap<>());
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
