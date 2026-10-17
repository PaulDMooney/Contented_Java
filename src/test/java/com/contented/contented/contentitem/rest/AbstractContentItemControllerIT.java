package com.contented.contented.contentitem.rest;

import com.contented.contented.contentitem.ContentItemVersionRepository;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

public abstract class AbstractContentItemControllerIT {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ContentItemVersionRepository contentItemRepository;

    protected RestTestClient contentItemEndpointClient;

    @BeforeAll()
    void beforeAll() {
        contentItemEndpointClient = createContentItemsEndpointClient(port);
    }

    public static RestTestClient createContentItemsEndpointClient(int port) {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentItemController.CONTENTITEMS_PATH);
        return RestTestClient.bindToServer().baseUrl(baseURL).build();
    }

}
