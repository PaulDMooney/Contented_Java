package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpParser;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class SearchResponseDeserializer<T> extends StdDeserializer<SearchResponse<T>> {

    public SearchResponseDeserializer() {
        this(null);
    }

    public SearchResponseDeserializer(Class<T> searchResponseClass) {
        super(searchResponseClass);
    }

    @Override
    public SearchResponse<T> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        ObjectMapper objectMapper = (ObjectMapper) jsonParser.getCodec();
        JacksonJsonpMapper mapper = new JacksonJsonpMapper(objectMapper);
        JacksonJsonpParser parser = new JacksonJsonpParser(jsonParser, mapper);
        return new SearchResponse.Builder<T>().withJson(parser, mapper).build();
    }
}
