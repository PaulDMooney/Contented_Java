package com.contented.contented.contentlet;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
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
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
            void when() {

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

    @Nested
    @DisplayName("GET /{id} endpoint")
    class GetByIdEndPoint {

		@Nested
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		@DisplayName("given a contentlet with fields was saved")
		class GivenAContentletWithFieldsWasSaved {
			// Given
			SomethingThatLooksLikeAContentlet toSave =
					new SomethingThatLooksLikeAContentlet("Contentlet2",
							"field1Value", 123);

			@BeforeAll
			void beforeAll() {

				// TBD: Save directly to the DB instead?
				// When
				WebTestClient.ResponseSpec response = webTestClient.put().bodyValue(toSave).exchange();

				response.expectStatus().is2xxSuccessful();
			}

			@Nested
			@TestInstance(TestInstance.Lifecycle.PER_CLASS)
			@DisplayName("when getting that contentlet with fields")
			class GetAContentlet {

				WebTestClient.ResponseSpec response;

				@BeforeAll
				void beforeAll() {
					// When
					response = webTestClient.get()
							.uri("/" + toSave.id())
							.exchange();
				}

				@Test
				@DisplayName("it should return the contentlet with its fields")
				void it_should_return_the_contentlet_with_its_fields() {

					// Then
					response.expectStatus().is2xxSuccessful()
							.expectBody(SomethingThatLooksLikeAContentlet.class)
							.value(contentlet -> {
								assertThat(contentlet.id()).isEqualTo(toSave.id());
								assertThat(contentlet.field1()).isEqualTo(toSave.field1());
								assertThat(contentlet.field2()).isEqualTo(toSave.field2());
							});
				}
			}
		}

		@Nested
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		@DisplayName("given a contentlet with complex fields was saved")
		class GivenAContentletWithComplexFieldsWasSaved {

			record ContentletWithComplexFields(String id, List<String> strings, List<ComplexField> stuff) {
			}

			record ComplexField(String field1, int field2) {
			}

			ContentletWithComplexFields toSave = new ContentletWithComplexFields(
					"Contentlet3",
					List.of("string1", "string2"),
					List.of(new ComplexField("field1Value", 123), new ComplexField("field2Value", 456))
			);

			@BeforeAll
			void given() {

				// Given
				webTestClient.put().bodyValue(toSave).exchange()
						.expectStatus().is2xxSuccessful();

			}

			@Nested
			@TestInstance(TestInstance.Lifecycle.PER_CLASS)
			@DisplayName("when getting that contentlet with complex fields")
			class GetContentletWithComplexFields {

				WebTestClient.ResponseSpec response;

				@BeforeAll
				void when() {

					// When
					response = webTestClient.get()
							.uri("/" + toSave.id())
							.exchange();

				}

				@Test
				@DisplayName("it should return the contentlet with its complex fields")
				void it_should_return_the_contentlet_with_its_complex_fields() {

					// Then
					response.expectStatus().is2xxSuccessful()
							.expectBody(ContentletWithComplexFields.class)
							.value(contentlet -> {
								assertThat(contentlet.id()).isEqualTo(toSave.id());
								assertThat(contentlet.strings()).isEqualTo(toSave.strings());
								assertThat(contentlet.stuff()).isEqualTo(toSave.stuff());
							});
				}
			}

		}

    }

    record SomethingThatLooksLikeAContentlet(String id, String field1, int field2) {
    }
}
