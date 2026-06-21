---
name: spring-boot-testing
description: Guidelines for writing Spring Boot tests in this project. Use when writing, reviewing, or refactoring tests.
---

# Testing Guidelines

## Philosophy

Test output is documentation. A reader should understand the system's expected behavior from test names and structure alone, without reading test code. Optimize for **report readability** — the output shown in IDE test runners, CI dashboards, and test reports.

## Test Types

This document uses three categories of tests:

- **Solitary unit tests** — Test a single unit in complete isolation. All dependencies are mocks or spies. Typically not a `@SpringBootTest`.
- **Sociable unit tests** — Test multiple units working together within the application but **do not cross IO boundaries to external systems**. Often a `@SpringBootTest` for convenience of wiring, but no TestContainers. Exceptions to the "no network calls" rule:
    - There is a test client (e.g. `WebTestClient`) connecting to the application itself via `localhost:{port}`.
    - In-process servers spun up for the test: OkHttp `MockWebServer`, H2 database, Flapdoodle (in-memory MongoDB), etc.
- **Integration tests** — Test that components work together **across IO boundaries** (database via TestContainers, Kafka, Redis, HTTP calls, etc.). Always a `@SpringBootTest`.

## Test Structure

Organize tests as a hierarchy of nested contexts terminating in assertions. Use the **UnitUnderTest > Given > When > Then** pattern:

```
ClassUnderTest                                 ← class or file being tested
└─ methodUnderTest()                           ← method, endpoint, or concern being tested
   └─ Given {preconditions}                    ← optional
      └─ And {additional preconditions}        ← optional
         └─ When {action with inputs}          ← optional for stateless/simple cases
            └─ And {additional inputs}          ← optional
               ├─ It should {expected outcome}  ← required, one per test
               └─ It should {other outcome}     ← optional
```

### When to use each level

| Level | Use when                                                                       | Skip when |
|-------|--------------------------------------------------------------------------------|-----------|
| **Given** | World state, pre-existing data, or configuration affects the expected behavior. **Never** for method inputs — those belong in When. | Testing a pure/stateless function with no preconditions |
| **And** (under Given) | Multiple independent preconditions combine | A single Given clause covers it |
| **When** | You want to describe the action being performed, or the inputs vary for the same preconditions | The action is obvious from the method-under-test name and has no meaningful inputs |
| **And** (under When) | Multiple independent inputs combine | A single When clause covers it |

### Writing good descriptions

- **Be specific.** A reader should understand behavior without seeing code. Avoid vague terms like "appropriate value" or "correct result."
- **Given is for world state, When is for method inputs.** Given describes state that exists *before* the method is called — database records, configuration, dependency behavior. Arguments passed directly to the method under test are part of the action and belong in a When clause. "When called with a properties map containing namespaced keys" is correct; "Given a properties map with namespaced keys" is wrong because there is no pre-existing state — you're just describing the input.
- **Describe relationships between contexts.** When an input relates to a precondition, make that connection explicit. "When called with the `id` of an existing `User`" is better than "When called with a valid `id`" — it tells the reader *which* id and *why* it matters.
- **Lead with world state, not responses.** If a Given clause starts with a service or dependency name followed by "returns" or "responds", it is describing a *consequence*, not the world state that causes it. Rewrite it as what is true about the world, then optionally append the technical detail. "Given the dependent service has no data for the `dmsId`" is correct; "Given the dependent service returns `204 No Content`" is wrong. If the technical detail aids readability, append it: "Given the dependent service has no data for the `dmsId`, for which it would respond with a `204`".
- **Omit literal values** unless semantically important. Enums, booleans, boundary values, and parameterized test values are exceptions.
- **Use backticks** for code references: method names, parameter names, types, endpoints.

## JUnit Implementation

Use JUnit Jupiter with `@Nested` classes and `@DisplayName` annotations. See
[examples.md](examples.md) for a full worked service test showing the
Given/When/Then nesting, plus a REST endpoint test and a spy-verification example.

### Conventions

- **Assertions:** Use AssertJ (`assertThat`), not JUnit assertions.
- **Comments:** Use `// Given`, `// When`, `// Then` to mark setup, execution, and assertion blocks within test methods.
- **Integrations:** Use TestContainers for dependent services like databases, Kafka, Redis, etc.
- **REST controllers:** Use `WebTestClient` to interact with REST endpoints to cover the experience of an actual client of the REST endpoint including networking, HTTP protocol, headers, and how data is serialized.
- **External REST dependencies:** If a dependent web/REST service isn't available via TestContainers, use OkHttp's `MockWebServer` to mimic its responses.
- **Async testing:** Use Awaitility for asserting on asynchronous operations (e.g. waiting for a Kafka message to be consumed, a record to appear in the database, etc.).
- **Mocking Spring beans:** In `@SpringBootTest`-based tests, use `@MockitoBean` / `@MockitoSpyBean` (Spring Boot 3.4+) to replace beans in the application context. The older `@MockBean` / `@SpyBean` are deprecated and should not be used in new tests. For solitary unit tests with no Spring context, use plain Mockito (`@Mock` / `@Spy` with `MockitoExtension`, or `mock(...)` / `spy(...)` directly).
- **Test data:** Prefer Instancio for randomized test data where possible — it reduces boilerplate and can surface edge cases. Randomization becomes a liability when the test asserts on specific field values, relationships between fields, or formats that random data won't satisfy — in those cases, either pin the relevant fields explicitly via Instancio's `set()`/`generate()` or fall back to a builder. Reach for a real sample (a JSON/CSV/SQL fixture sourced from a live system) when:
    - The Instancio setup to make the data realistic would be more complex than just checking in a sample.
    - The data has inferred or cross-field relationships Instancio can't reasonably reproduce (denormalized fields, derived IDs, encoded payloads, domain invariants).
    - Seeing a concrete, recognizable record in the test makes the scenario clearer to the reader than randomized data would.

  Choose based on how true-to-life the data needs to be vs. the benefit of randomization.
- **Coverage:** Aim for full line, statement, and branch coverage on every new unit — directly via its own tests if public/shared, or indirectly through tests of a dependent unit if private. Where a service enforces a coverage threshold (e.g. JaCoCo in the Maven/Gradle build), that threshold is the floor, not the target.
- **Lifecycle:** Set `junit.jupiter.testinstance.lifecycle.default=per_class` in `src/test/resources/junit-platform.properties`. This enables `@BeforeAll`/`@AfterAll` on non-static methods and supports instance state shared across tests in a nested class.
- **Setup/teardown:** Prefer `@BeforeAll`/`@AfterAll` over `@BeforeEach`/`@AfterEach` when possible to avoid redundant setup and optimize test execution. **Watch for sibling bleed:** with `per_class` lifecycle, state mutated by a `@BeforeAll` in one nested context can persist into sibling contexts that share the same parent instance (database rows, mock stubs, static fields, etc.). Either scope state to the nested class that owns it, or clean up in `@AfterAll` so siblings start from a known baseline.
- **Configuration:** When config changes unit behavior, write separate tests per configuration.
- **Inner class names:** Match the nesting level — `GivenX`, `AndY`, `WhenZ`. Keep them short but descriptive.
- **Method names:** Use descriptive `should*` names on `@Test` methods. The `@DisplayName` is authoritative; the method name is a secondary signal.
- **Multiple test files per unit:** It's fine to have multiple test class files for the same class or unit. Split when the file is getting too large, or to separate integration tests from sociable tests from solitary tests. Naming convention:
    - `PersonServiceTest.java` — solitary unit tests (default)
    - `PersonServiceSociableTest.java` — sociable unit tests
    - `PersonServiceIT.java` — integration tests (`*IT` suffix aligns with Maven Failsafe)
    - If splitting by concern within the same test type: `PersonServiceDeleteTest.java`, `PersonServiceSearchTest.java`

## REST Endpoint Tests

For controller/endpoint tests, use the endpoint as the unit under test — the
`@Nested` class is named for the endpoint (e.g. ``"`DELETE /api/persons/{id}` endpoint"``)
rather than a method. See [examples.md](examples.md) for a full controller test.

## When to Use `@SpringBootTest`

Use `@SpringBootTest` for sociable unit and integration tests that need the Spring context:
- **Services** — testing Spring-managed beans with their dependencies
- **REST controllers** — endpoint tests via `WebTestClient`
- **Spring integration mechanisms** — Kafka listeners/publishers, Spring Retry, etc.
- **TestContainers** — any test involving a real database, Kafka broker, Redis, etc.

Do **not** use `@SpringBootTest` for solitary unit tests (e.g. utility methods, pure logic) — just instantiate the class directly and mock dependencies.

### Disable unused parts of the application

`@SpringBootTest` loads the full application context by default. Disable components not relevant to the test to keep tests fast and avoid unnecessary infrastructure. The mechanisms:

- **Disable the web environment** (`webEnvironment = WebEnvironment.NONE`) when not testing REST endpoints.
- **Use a test profile** via a `@TestProfile` meta-annotation to activate test-specific configuration.
- **Custom annotations** (e.g. `@DisableDatabase`) for coarse-grained auto-configuration exclusions.
- **Application properties** to toggle specific features per test.

See [springboot-config.md](springboot-config.md) for the code defining each of these. Use whichever mechanism fits — the goal is that each test only starts the infrastructure it actually exercises.

## Integration/Sociable Tests vs Solitary Tests

### When to write integration or sociable tests

Write at least some integration or sociable unit tests to prove that components work together:

- **REST controllers:** At least one integration test per controller to verify that an endpoint reaches the service layer, persists data, and returns the expected response.
- **Service transactional boundaries:** When a service calls repositories to modify data, integration tests with a real database (via TestContainers) prove that transactions commit and roll back as expected.
- **Spring wiring:** Any path where correct behavior depends on Spring configuration, bean injection, or infrastructure (Kafka, retry, etc.) deserves an integration or sociable unit test.

The goal is confidence that a realistic path through the application works. These tests are broader and slower — you don't need one for every scenario, just enough to cover the critical paths.

### When to use mocks and spies (solitary unit tests)

Once integration/sociable tests have established that components are wired correctly, use mocks and spies in solitary unit tests to cover the various logical scenarios in isolation:

- **Mocks** for dependencies whose behavior you want to control (e.g. stubbing a repository to return specific data) without involving real infrastructure.
- **Spies** when you need to verify that a dependency's method was called with expected arguments.

Use solitary unit tests when involving real infrastructure or the full Spring context would be redundant or tedious — e.g. the database interaction is already covered by integration tests, and you just need to verify the service's branching logic.

### Spy verification in test descriptions

When a test uses a spy to verify a method call, **name the method in the `@DisplayName`** (e.g. ``"It should pass the `id` to `PersonRepository#deleteById()`"``). This connects the mocked test to the test that covers the spy's real implementation — the reader can look up the tests for `PersonRepository#deleteById()` to see how the real implementation is covered. See [examples.md](examples.md) for the code.

## Common Mistakes

- **Putting non-state in "Given" clauses.** Given describes pre-existing world state — nothing else. See [Writing good descriptions](#writing-good-descriptions) for the full rules and rewrites. Quick checks:
    - **Not technical responses.** If your Given starts with a service/dependency name followed by "returns" or "responds", it's a consequence, not state — rewrite it.
    - **Not method inputs.** Arguments passed directly to the method under test belong in a When clause, not a Given.
- **Repeating context in nested descriptions.** Each level inherits parent context. Don't write "Given a Person exists, when deleting the Person that exists" — the When inherits the Given.
- **Too many assertions per test.** Each `@Test` should verify one logical expectation. Split compound assertions into separate tests.
- **Vague display names.** "It should work correctly" or "It should return the right value" tells the reader nothing.
- **Skipping `@DisplayName`.** Every `@Test`, `@Nested` class must have a `@DisplayName`. Method names alone are not readable enough in reports.