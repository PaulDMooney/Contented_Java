package com.contented.contented.contentitem.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.elasticsearch.SearchResponseDeserializer;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

@DisplayName("SearchResponseDeserializer")
public class SearchResponseDeserializerTests {

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
                        "_index": "contentitemindex",
                        "_id": "my_id_124",
                        "_score": 1.0,
                        "_source": {
                            "id": "my_id_124"
                        }
                    },
                    {
                        "_index": "contentitemindex",
                        "_id": "my_id_123",
                        "_score": 1.0,
                        "_source": {
                            "id": "my_id_123"
                        }
                    }
                ]
            }
        }""";

    @Nested
    @DisplayName("Given there is no SearchResponseDeSerializer registered with the objectMapper")
    public class NoDeSerializerRegisteredTests {

        @Test
        @DisplayName("it should not deserialize a SearchResponse from JSON")
        public void it_should_not_deserialize_a_SearchResponse_from_JSON() {
            var objectMapper = JsonMapper.builder().build();

            Assertions.assertThatThrownBy(() -> objectMapper.readValue(exampleSearchResponse, SearchResponse.class))
                .isInstanceOf(DatabindException.class);

        }
    }

    @Nested
    @DisplayName("Given there is a SearchResponseDeserializer registered with the objectMapper")
    public class DeSerializerRegisteredTests {

        @Test
        @DisplayName("it should deserialize a SearchResponse from JSON")
        public void it_should_deserialize_a_SearchResponse_from_JSON() {
            var module = new SimpleModule();
            module.addDeserializer(SearchResponse.class, new SearchResponseDeserializer());
            var objectMapper = JsonMapper.builder().addModule(module).build();

            var searchResponse = objectMapper.readValue(exampleSearchResponse, SearchResponse.class);

            Assertions.assertThat(searchResponse).isNotNull();
            Assertions.assertThat(searchResponse.hits()).isNotNull();
            Assertions.assertThat(searchResponse.hits().hits()).isNotNull();
            Assertions.assertThat(searchResponse.hits().hits()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("When deserializing an Object with a SearchResponseDeserializer annotated field")
    class DeserializingViaAnnotation {

        final String wrapperJSON = """
        {
            "searchResponse": %s
        }
""";
        record SearchResponseAnnotated(
            @JsonDeserialize(using = SearchResponseDeserializer.class) SearchResponse<?> searchResponse) {}

        @Test
        @DisplayName("it should deserialize a SearchResponse from JSON")
        public void it_should_deserialize_a_SearchResponse_from_JSON() {
            var searchResponseAnnotatedJSON = String.format(wrapperJSON, exampleSearchResponse);
            var objectMapper = JsonMapper.builder().build();

            var annotatedObject = objectMapper.readValue(searchResponseAnnotatedJSON, SearchResponseAnnotated.class);

            Assertions.assertThat(annotatedObject).isNotNull();
            Assertions.assertThat(annotatedObject.searchResponse).isNotNull();
            Assertions.assertThat(annotatedObject.searchResponse.hits()).isNotNull();
            Assertions.assertThat(annotatedObject.searchResponse.hits().hits()).isNotNull();
            Assertions.assertThat(annotatedObject.searchResponse.hits().hits()).hasSize(2);
        }
    }
}
