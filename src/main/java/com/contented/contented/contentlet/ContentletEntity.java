package com.contented.contented.contentlet;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
public class ContentletEntity {

    @Id
    private String id;

    @JsonIgnore
    private Map<String, Object> schemalessData = new LinkedHashMap<>();

    public ContentletEntity(String id) {
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

    public <T extends Object> T get(String key) {
        return (T) schemalessData.get(key);
    }

}
