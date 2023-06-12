package com.contented.contented.contentlet.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticSearchContainerUtils {

    public static ElasticsearchContainer elasticsearchContainer() {

        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.10")
            .withExposedPorts(9200);
    }
    public static void startAndRegisterElasticsearchContainer(ElasticsearchContainer elasticsearchContainer, DynamicPropertyRegistry registry) {
        elasticsearchContainer.start();

        // If we setup the reactiveclient manually, then this property needs to change to match
        // what we use in that setup.
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
    }
}
