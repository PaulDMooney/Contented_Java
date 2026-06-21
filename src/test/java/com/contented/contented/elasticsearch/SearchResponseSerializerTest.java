package com.contented.contented.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchResponseSerializer")
public class SearchResponseSerializerTest {

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

    SearchResponse<EntityAsMap> exampleSearchResponseObj = new SearchResponse.Builder<EntityAsMap>()
        .withJson(new ByteArrayInputStream(exampleSearchResponse.getBytes()))
        .build();

    @Nested
    @DisplayName("`serialize()`")
    class Serialize {

        String serialized;

        @BeforeAll
        void when() {
            // The serializer is only reachable through a Jackson ObjectMapper it is registered on.
            var module = new SimpleModule();
            module.addSerializer(SearchResponse.class, new SearchResponseSerializer(new Jackson3JsonpMapper()));
            var objectMapper = JsonMapper.builder().addModule(module).build();

            serialized = objectMapper.writeValueAsString(exampleSearchResponseObj);
        }

        @Test
        @DisplayName("It should write each hit of the `SearchResponse` to JSON")
        void it_should_write_each_hit() {
            assertThat(serialized).contains("my_id_124", "my_id_123");
        }
    }
}
