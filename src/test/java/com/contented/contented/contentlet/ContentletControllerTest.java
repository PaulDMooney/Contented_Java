package com.contented.contented.contentlet;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContentletControllerTest {

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @BeforeAll
    void beforeAll() {
        var baseURL = String.format("http://localhost:%s/%s", port, ContentletController.CONTENTLETS_PATH);
        webTestClient = WebTestClient.bindToServer().baseUrl(baseURL).build();
    }

    @Nested
    @DisplayName("GET /all endpoint")
    class GetALLEndpoint {

        @Test
        @DisplayName("should return all saved contentlets")
        void should_return_all_saved_contentlets() {

            // When
            var response = webTestClient.get().uri("/all").exchange();

            // Then
            response.expectStatus()
                    .is2xxSuccessful();
        }
    }
}
