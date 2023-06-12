package com.contented.contented.contentlet.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;

public class MongoDBContainerUtils {

    public static MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer("mongo:6.0")
            .withExposedPorts(27017);
    }

    public static void startAndRegsiterMongoDBContainer(MongoDBContainer mongoDBContainer, DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}
