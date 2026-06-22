# Trimming the `@SpringBootTest` context

`@SpringBootTest` loads the full application context by default. Disable components not relevant to the test to keep tests fast and avoid unnecessary infrastructure. Code snippets for the mechanisms referenced in `SKILL.md`.

## Disable the web environment

When not testing REST endpoints:

```java
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
```

## Test profile

Activate test-specific configuration via a `@TestProfile` meta-annotation (a convenient stand-in for `@ActiveProfiles("test")`):

```java
/**
 * Convenient stand-in for @ActiveProfiles with the value "test".
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles({"test"})
public @interface TestProfile {}
```

```java
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@TestProfile
@DisableDatabase
class MyServiceTest { }
```

## Custom annotations for coarse-grained exclusions

```java
/**
 * Disables DataSource and Liquibase auto-configuration.
 * Use for tests that do not require a database connection.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
public @interface DisableDatabase {}
```

## Application properties for toggling features

```java
@SpringBootTest(properties = {
    "kafka.my-service.consumer.enabled=true"
})
class KafkaListenerTest { }
```

Use whichever mechanism fits — the goal is that each test only starts the infrastructure it actually exercises.
