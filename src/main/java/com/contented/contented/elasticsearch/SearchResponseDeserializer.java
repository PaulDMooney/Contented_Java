package com.contented.contented.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

@JacksonComponent
public class SearchResponseDeserializer<T> extends StdDeserializer<SearchResponse<T>> {

    private final Jackson3JsonpMapper jsonpMapper;

    @Autowired
    public SearchResponseDeserializer(Jackson3JsonpMapper jsonpMapper) {
        super(SearchResponse.class);
        this.jsonpMapper = jsonpMapper;
    }

    @Override
    public SearchResponse<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        Jackson3JsonpParser parser = new Jackson3JsonpParser(jsonParser, jsonpMapper);
        return new SearchResponse.Builder<T>().withJson(parser, jsonpMapper).build();
    }
}
