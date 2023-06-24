package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchResponseSerializer")
public class SearchResponseSerializerTests {

    String exampleSearchResponse  = """
        {
            "took": 156,
            "timed_out": false,
            "_shards": {
                "total": 1,
                "successful": 1,
                "skipped": 0,
                "failed": 0
            },
            "hits": {
                "total": {
                    "value": 2,
                    "relation": "eq"
                },
                "max_score": 1.0,
                "hits": [
                    {
                        "_index": "contentletindex",
                        "_id": "my_id_124",
                        "_score": 1.0,
                        "_source": {
                            "id": "my_id_124"
                        }
                    },
                    {
                        "_index": "contentletindex",
                        "_id": "my_id_123",
                        "_score": 1.0,
                        "_source": {
                            "id": "my_id_123"
                        }
                    }
                ]
            }
        }""";

    SearchResponse<EntityAsMap> exampleSearchResponseObj = new SearchResponse.Builder<EntityAsMap>()
        .withJson(new ByteArrayInputStream(exampleSearchResponse.getBytes()))
        .build();

    @Nested
    @DisplayName("Given there is no SearchResponseSerializer registered with the objectMapper")
    public class NoSerializerRegisteredTests {

        @Test
        @DisplayName("it should not serialize a SearchResponse to JSON")
        public void it_should_not_serialize_a_SearchResponse_to_JSON() throws JsonProcessingException {
            var objectMapper = new ObjectMapper();
            String serialized = objectMapper.writeValueAsString(exampleSearchResponseObj);
            assertThat(serialized).isEqualTo("{}");
        }

    }

    @Nested
    @DisplayName("Given there is a SearchResponseSerializer registered with the objectMapper")
    public class SerializerRegisteredTests {

        @Test
        @DisplayName("it should serialize a SearchResponse to JSON")
        public void it_should_serialize_a_SearchResponse_to_JSON() throws JsonProcessingException {
            var objectMapper = new ObjectMapper();
            var module = new SimpleModule();
            module.addSerializer(SearchResponse.class, new SearchResponseSerializer());
            objectMapper.registerModule(module);

            String serialized = objectMapper.writeValueAsString(exampleSearchResponseObj);
            assertThat(serialized).isNotBlank();
            assertThat(serialized).isNotEqualTo("{}");
            assertThat(serialized).contains("\"hits\""); // an expected property inside of SearchResponse
        }

    }

    @Nested
    @DisplayName("When serializing an Object with a SearchResponseSerializer annotated field")
    class SerializingViaAnnotation {

        record ToSerialize(@JsonSerialize(using = SearchResponseSerializer.class) SearchResponse myResponse) {}

        @Test
        @DisplayName("it should serialize the SearchResponse field")
        public void it_should_serialize_the_SearchResponse_field() throws JsonProcessingException {
            var objectMapper = new ObjectMapper();

            var toSerialize = new ToSerialize(exampleSearchResponseObj);
            String serialized = objectMapper.writeValueAsString(toSerialize);
            assertThat(serialized).isNotBlank();
            assertThat(serialized).isNotEqualTo("{}");
            assertThat(serialized).contains("\"hits\""); // an expected property inside of SearchResponse
        }
    }

    @Nested
    @DisplayName("serialize")
    public class SerializeTests {

        @Test
        @DisplayName("SearchResponse does not serialize on its own")
        public void SearchResponse_does_not_serialize_on_its_own() throws JsonProcessingException {
            var objectMapper = new ObjectMapper();
            var result = objectMapper.writeValueAsString(exampleSearchResponseObj);
            assertThat(result).isEqualTo("{}");
        }

        @Test
        @DisplayName("it should serialize a SearchResponse to JSON")
        public void it_should_serialize_a_SearchResponse_to_JSON() throws JsonProcessingException {
            var objectMapper = new ObjectMapper();
            var module = new SimpleModule();
            module.addSerializer(SearchResponse.class, new SearchResponseSerializer());
            objectMapper.registerModule(module);

            String serialized = objectMapper.writeValueAsString(exampleSearchResponseObj);
            assertThat(serialized).isNotBlank();
            assertThat(serialized).isNotEqualTo("{}");
            assertThat(serialized).contains("\"hits\"");
        }

    }
}
