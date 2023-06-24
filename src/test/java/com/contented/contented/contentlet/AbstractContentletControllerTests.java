package com.contented.contented.contentlet;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

public abstract class AbstractContentletControllerTests {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ContentletRepository contentletRepository;

    protected WebTestClient contentletEndpointClient;

    @BeforeAll()
    void beforeAll() {
        contentletEndpointClient = createContentletsEndpointClient(port);
    }

    public static WebTestClient createContentletsEndpointClient(int port) {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        return WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

}
