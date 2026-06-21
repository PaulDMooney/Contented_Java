package com.contented.contented.contentitem;

import com.contented.contented.contentitem.rest.ContentItemController;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

public abstract class AbstractContentItemControllerTests {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ContentItemRepository contentItemRepository;

    protected WebTestClient contentItemEndpointClient;

    @BeforeAll()
    void beforeAll() {
        contentItemEndpointClient = createContentItemsEndpointClient(port);
    }

    public static WebTestClient createContentItemsEndpointClient(int port) {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentItemController.CONTENTITEMS_PATH);
        return WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

}
