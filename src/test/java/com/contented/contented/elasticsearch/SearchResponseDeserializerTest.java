package com.contented.contented.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchResponseDeserializer")
public class SearchResponseDeserializerTest {

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
    @DisplayName("`deserialize()`")
    class Deserialize {

        SearchResponse<?> result;

        @BeforeAll
        void when() {
            // The deserializer is only reachable through a Jackson ObjectMapper it is registered on.
            var module = new SimpleModule();
            module.addDeserializer(SearchResponse.class, new SearchResponseDeserializer<>(new Jackson3JsonpMapper()));
            var objectMapper = JsonMapper.builder().addModule(module).build();

            result = objectMapper.readValue(exampleSearchResponse, SearchResponse.class);
        }

        @Test
        @DisplayName("It should deserialize each hit from the JSON into the `SearchResponse`")
        void it_should_deserialize_each_hit() {
            var hitIds = result.hits().hits().stream().map(hit -> hit.id()).toList();

            assertThat(hitIds).containsExactly("my_id_124", "my_id_123");
        }
    }
}
