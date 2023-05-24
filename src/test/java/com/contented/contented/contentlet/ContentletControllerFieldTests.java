package com.contented.contented.contentlet;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@DisplayName("ContentletController field tests")
public class ContentletControllerFieldTests extends AbstractContentletControllerTests {

    // ContentletRepository needs a MongoDB to communicate with
    @Container
    static MongoDBContainer mongoDBContainer = mongoDBContainer();

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {
        startAndRegsiterMongoDBContainer(mongoDBContainer, registry);
    }

    @Nested
    @DisplayName("PUT endpoint")
    class PutEndPoint {

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("when saving a contentlet with fields")
        class SaveANewContentlet {

            // Given
            SomethingThatLooksLikeAContentlet toSave =
                    new SomethingThatLooksLikeAContentlet("Contentlet1",
                            "field1Value", 123);

            WebTestClient.ResponseSpec response;

            @BeforeAll
            void beforeAll() {

                // When
                response = webTestClient.put().bodyValue(toSave).exchange();

                response.expectStatus().is2xxSuccessful();
            }

            @Test
            @DisplayName("it should save the contentlet with its given fields")
            void it_should_save_the_contentlet_with_its_given_fields() {

                ContentletEntity savedEntity = contentletRepository.findById(toSave.id()).block();

                assertThat((String) savedEntity.get("field1")).isEqualTo(toSave.field1());
                assertThat((Integer) savedEntity.get("field2")).isEqualTo(toSave.field2());
            }
        }
    }

    record SomethingThatLooksLikeAContentlet(String id, String field1, int field2) {
    }
}
