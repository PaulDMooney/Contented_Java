package com.contented.contented.contentitem.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticSearchContainerUtils {

    public static ElasticsearchContainer elasticsearchContainer() {

        // Keep in step with the elasticsearch-java client version managed by the Spring Boot parent
        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.4.2")
            .withEnv("xpack.security.enabled", "false")
            .withExposedPorts(9200);
    }
    public static void startAndRegisterElasticsearchContainer(ElasticsearchContainer elasticsearchContainer, DynamicPropertyRegistry registry) {
        elasticsearchContainer.start();

        // If we setup the reactiveclient manually, then this property needs to change to match
        // what we use in that setup.
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
    }
}
