package com.contented.contented.contentlet.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@Configuration
public class ElasticSearchConfig {

    public static final String INDEX_PROPERTY_KEY = "elasticsearch.index.name";

    @Bean
    public IndexCoordinates indexCoordinates(@Value("${"+ INDEX_PROPERTY_KEY + "}") String indexName) {
        return IndexCoordinates.of(indexName);
    }
}
