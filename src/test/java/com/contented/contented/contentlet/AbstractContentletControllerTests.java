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

    @BeforeAll()
    void beforeEach() {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        webTestClient = WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected ContentletRepository contentletRepository;

    // ContentletRepository needs a MongoDB to communicate with
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withExposedPorts(27017);

    WebTestClient webTestClient;

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {

        mongoDBContainer.start();
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}
