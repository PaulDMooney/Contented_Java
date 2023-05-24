package com.contented.contented.contentlet;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

public class AbstractContentletControllerTests {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ContentletRepository contentletRepository;

    protected WebTestClient webTestClient;

    @BeforeAll()
    void beforeEach() {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        webTestClient = WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

    protected static void startAndRegsiterMongoDBContainer(MongoDBContainer mongoDBContainer, DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    protected static MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer("mongo:6.0")
                .withExposedPorts(27017);
    }

}
