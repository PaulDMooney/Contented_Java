package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class SearchResponseSerializer<T> extends StdSerializer<SearchResponse<T>> {

    public SearchResponseSerializer() {
        this(null);
    }

    public SearchResponseSerializer(Class<SearchResponse<T>> t) {
        super(t);
    }

    @Override
    public void serialize(SearchResponse<T> searchResponse, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        ObjectMapper objectMapper = (ObjectMapper) jsonGenerator.getCodec();
        JacksonJsonpGenerator generator = new JacksonJsonpGenerator(jsonGenerator);
        JacksonJsonpMapper mapper = new JacksonJsonpMapper(objectMapper);
        searchResponse.serialize(generator, mapper);
    }
}
