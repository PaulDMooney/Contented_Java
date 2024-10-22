package com.contented.contented.contentlet;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;

public abstract class AbstractContentletControllerTests {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ContentletRepository contentletRepository;

    protected TestRestTemplate contentletEndpointClient;

    @BeforeAll()
    void beforeAll() {
        contentletEndpointClient = createContentletsEndpointClient(port);
    }

    public static TestRestTemplate createContentletsEndpointClient(int port) {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        return new TestRestTemplate().withBasicAuth(baseURL);
    }

}
