package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.Jackson3JsonpGenerator;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

@JacksonComponent
public class SearchResponseSerializer<T> extends StdSerializer<SearchResponse<T>> {

    private final Jackson3JsonpMapper jsonpMapper;

    // For instantiation by Jackson via @JsonSerialize(using = ...), where injection isn't possible
    public SearchResponseSerializer() {
        this(new Jackson3JsonpMapper());
    }

    @Autowired
    public SearchResponseSerializer(Jackson3JsonpMapper jsonpMapper) {
        super(SearchResponse.class);
        this.jsonpMapper = jsonpMapper;
    }

    @Override
    public void serialize(SearchResponse<T> searchResponse, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        Jackson3JsonpGenerator generator = new Jackson3JsonpGenerator(jsonGenerator);
        searchResponse.serialize(generator, jsonpMapper);
    }
}
