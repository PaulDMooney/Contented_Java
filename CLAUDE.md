# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4 (MVC, blocking, virtual threads enabled) content management API on Java 25. Contentlets — schemaless content records — are persisted to MongoDB (system of record) and indexed into Elasticsearch for search. Maven build via the wrapper (`./mvnw`). JSON is Jackson 3 (`tools.jackson` packages; only the `com.fasterxml.jackson.annotation.*` annotations kept their Jackson 2 package).

## Commands

```bash
./mvnw test                          # run all tests (requires Docker — tests use Testcontainers)
./mvnw test -Dtest=ContentletServiceTest                  # single test class
./mvnw test -Dtest=ContentletServiceTest#someMethod       # single test method
./mvnw spring-boot:run               # run the app (start docker-compose services first)
docker-compose up -d                 # local MongoDB (27017), mongo-express UI (8081), Elasticsearch (9200)
```

CI runs `mvn --batch-mode surefire-report:report`.

Most tests are integration tests (tagged `IntegrationTest`, see `TestTypeTags`) that spin up real MongoDB/Elasticsearch containers via Testcontainers, so Docker must be running even for `./mvnw test`.

## Architecture

The core flow is a dual-write pipeline triggered by `PUT /contentlets` (`ContentletController` → `ContentletService.save`):

1. **Inbound transformation** — `TransformationHandler` runs the entity through the *first* `ContentletEntityTransformer` bean whose `test()` predicate matches (e.g. `StandardDMSContentTransformer`, which normalizes legacy DMS fields like `stName`/`contentType`/`inode`/`identifier` and derives the id). No match = entity passes through unchanged.
2. **MongoDB save** — `ContentletRepository` (blocking `MongoRepository`). Existence is checked first so the controller can return 201 vs 200.
3. **Elasticsearch indexing** — `ContentletIndexer` picks the *first* matching `ESRecordTransformer` bean (e.g. `BlogTransformer`), which maps one contentlet to **one or more** `EntityAsMap` ES documents. No match = warning logged, nothing indexed (the Mongo save still succeeds).

Both transformer families are discovered by Spring `List<T>` injection of `@Component` beans and gated by `Predicate.test()` — to support a new content type, add a new transformer bean of either kind; no registration step exists.

`ContentletEntity` is schemaless: an `@Id` plus a `Map<String, Object>` exposed through Jackson's `@JsonAnySetter`/`@JsonAnyGetter`, so arbitrary JSON fields round-trip through the API and MongoDB.

Other entry points:
- `POST /search/withcontent` (`SearchController`) — accepts a raw Elasticsearch query JSON body, runs it against the index, then hydrates full contentlets from MongoDB by the ids found in the hits (`SearchResultsWithContent` carries both; it has custom Jackson serializers in the `elasticsearch` package).
- `PUT /index/create` (`IndexController`) — creates the ES index using `src/main/resources/elasticsearch/mappings.json`. Index name and mappings file come from `elasticsearch.index.*` in `application.yaml`; the index name is injected app-wide as a single `IndexCoordinates` bean (`ElasticSearchConfig`).

Swagger UI is available via springdoc at `/swagger-ui/index.html`.

## Test Conventions

Tests are BDD-style: `@Nested`/`@NestedPerClass` classes named for the scenario ("when saving a new contentlet"), `@DisplayName` on everything, given/when in `@BeforeAll`, one assertion per `@Test`. `@NestedPerClass` (in `testutils`) is `@Nested` + `@TestInstance(PER_CLASS)`, which is what allows `@BeforeAll` on instance methods.

Container setup is shared through `testutils.MongoDBContainerUtils` / `ElasticSearchContainerUtils`: declare a `@Container static` field and register its URI in a `@DynamicPropertySource` method. Controller tests extend `AbstractContentletControllerTests` for a `WebTestClient` bound to the contentlets endpoint (spring-webflux is a test-only dependency for this; replacing it with `RestClient`/`MockMvc` is part of roadmap item 4), and `@MockitoBean` the `ContentletIndexer` when ES isn't under test (stub helpers in `StubbingUtils`).
