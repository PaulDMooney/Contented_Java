# Worked Test Examples

Full code examples for the structure and conventions described in `SKILL.md`.

## Service test (Given/When/Then nesting)

A `@Nested`/`@DisplayName` hierarchy following the **UnitUnderTest > Given > When > Then** pattern:

```java
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PersonService")
class PersonServiceTest {

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Nested
        @DisplayName("Given a `Person` record exists in the database")
        class GivenPersonExists {

            @BeforeAll
            void given() {
                // Given
            }

            @Nested
            @DisplayName("When called with the `id` of that `Person`")
            class WhenCalledWithExistingId {

                @BeforeAll
                void when() {
                    // When
                }

                @Test
                @DisplayName("It should delete the `Person` record from the database")
                void shouldDeletePersonRecord() {
                    // Then
                }

                @Test
                @DisplayName("It should return `true`")
                void shouldReturnTrue() {
                    // Then
                }
            }

            @Nested
            @DisplayName("And the `Person` has an associated `Address`")
            class AndPersonHasAddress {

                @BeforeAll
                void andGiven() {
                    // Given
                }

                @Nested
                @DisplayName("When called with the `id` of that `Person`")
                class WhenCalledWithExistingId {

                    @Test
                    @DisplayName("It should delete both the `Person` and `Address` records")
                    void shouldDeletePersonAndAddress() {
                        // Then
                    }
                }
            }
        }

        @Nested
        @DisplayName("Given no `Person` record exists for the given id")
        class GivenPersonDoesNotExist {

            @Test
            @DisplayName("It should throw a `PersonNotFoundException`")
            void shouldThrowNotFoundException() {
                // When + Then
            }
        }
    }
}
```

## REST endpoint test

For controller/endpoint tests, use the endpoint as the unit under test:

```java
@DisplayName("PersonController")
class PersonControllerTest {

    @Nested
    @DisplayName("`DELETE /api/persons/{id}` endpoint")
    class DeletePersonEndpoint {

        @Nested
        @DisplayName("Given a `Person` record exists")
        class GivenPersonExists {

            @Nested
            @DisplayName("When called with the `id` for that `Person` record")
            class WhenCalledWithExistingPersonId {

                @Test
                @DisplayName("It should return `204 No Content`")
                void shouldReturn204() {
                }
            }
        }
    }
}
```

## Spy verification in test descriptions

When a test uses a spy to verify a method call, name the method in the `@DisplayName` so the reader can find the test that covers the spy's real implementation:

```java
@Test
@DisplayName("It should pass the `id` to `PersonRepository#deleteById()`")
void shouldCallDeleteById() {
    // Then
    verify(personRepository).deleteById(expectedId);
}
```
