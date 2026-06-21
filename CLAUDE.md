# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4 (MVC, blocking, virtual threads enabled) content management API on Java 25. ContentItems — schemaless content records — are persisted to Postgres (system of record) and indexed into Elasticsearch for search. Maven build via the wrapper (`./mvnw`). JSON is Jackson 3 (`tools.jackson` packages; only the `com.fasterxml.jackson.annotation.*` annotations kept their Jackson 2 package).

## Commands

```bash
./mvnw test                          # solitary unit tests only (Surefire, *Test) — fast, no Docker
./mvnw verify                        # unit + integration tests (Failsafe, *IT) — requires Docker (Testcontainers)
./mvnw test -Dtest=ContentItemServiceTest                  # single unit test class
./mvnw test -Dtest=ContentItemServiceTest#someMethod       # single unit test method
./mvnw verify -Dit.test=ContentItemServiceIT               # single integration test class (Failsafe uses -Dit.test)
./mvnw spring-boot:run               # run the app (start docker-compose services first)
docker-compose up -d                 # local Postgres (5432), pgAdmin UI (8081), Elasticsearch (9200)
```

CI runs `mvn --batch-mode verify` (unit + integration), then generates HTML reports via `surefire-report:report-only` + `surefire-report:failsafe-report-only`.

Tests are split by Maven naming convention: **solitary unit tests (`*Test`)** run under Surefire in the `test` phase with no infrastructure, so `./mvnw test` needs no Docker. **Integration tests (`*IT`** — also tagged `IntegrationTest`, see `TestTypeTags`**)** spin up real Postgres/Elasticsearch containers via Testcontainers and run under Failsafe in the `integration-test`/`verify` phases, so Docker must be running for `./mvnw verify`.

## Architecture

**Package layout.** Content-item-agnostic infrastructure lives in top-level packages — `common` (`UuidV7`), `persistence` (`JdbcConfig`, `SchemalessData`), and `elasticsearch` (generic ES infra: `ElasticSearchConfig`, `ElasticSearchIndexCreator`, `IndexController`, the `SearchResponse` (de)serializers). The domain lives under `contentitem`: `ContentItemService`/`ContentItemRepository` at its root, with sub-packages `rest` (the `@RestController`s — `ContentItemController` and `SearchController` — the exception handler, and the `SearchResultsWithContent` response type), `exceptions` (the domain exception types), `model` (entity, DTOs, mapper), `transformation` (inbound `ContentItemEntityTransformer`s + `TransformationHandler`), and `elasticsearch` (the content-aware indexer and `ESRecordTransformer`s — distinct from the generic top-level `elasticsearch`).

Ids are **intrinsic** (server-generated UUIDv7, assigned in `ContentItemService`); clients never supply them. The core flow is a dual-write pipeline with two write entry points:

- `POST /contentitems` (`ContentItemController.createContentItem` → `ContentItemService.create`) — create. A client-supplied body id is rejected with `400`; otherwise a UUIDv7 is generated and the response is `201 Created` with a `Location` header.
- `PUT /contentitems/{id}` (`ContentItemController.updateContentItem` → `ContentItemService.update`) — update only. The URL id is authoritative; a body id that disagrees with it is rejected with `400`, and an unknown id returns `404` (create-on-missing is deliberately disabled — creation is POST-only).

The persistence entity never leaves the service. `ContentItemService` accepts a `ContentItemDTO` and returns a `ContentItemResponseDTO` (reads too — `findById`/`findAll`/`findByIds` all return response DTOs); `ContentItemEntity` is created, saved, and mapped back entirely inside the service via `ContentItemMapper` (`toEntity`/`toResponse`). The controller only deserializes the request into a `ContentItemDTO`, performs the transport-level id checks (no id on POST, body-id-vs-URL-id on PUT), and forwards to the service — it never touches an entity or a repository. The request `ContentItemDTO` and response `ContentItemResponseDTO` share an `AbstractContentItemDTO` base holding `id`, `contentType`, and the schemaless map via Jackson's `@JsonAnySetter`/`@JsonAnyGetter`.

`contentType` is required on both writes — a create or update with a blank `contentType` is rejected with `400` — and is **immutable**: an update whose `contentType` differs from the stored one is rejected with `400`. All `400`/`404` responses carry an RFC 9457 `application/problem+json` body (`ContentItemExceptionHandler` maps `InvalidContentItemException`/`ContentItemNotFoundException` to `ProblemDetail`).

Both then run the same pipeline:

1. **Inbound transformation** — `TransformationHandler` runs the entity through the *first* `ContentItemEntityTransformer` bean whose `test()` predicate matches (e.g. `StandardDMSContentTransformer`, which normalizes `language` and stamps `modDate`; it gates on `contentType`). No match = entity passes through unchanged.
2. **Postgres save** — `ContentItemRepository` (Spring Data JDBC `ListCrudRepository`). `ContentItemEntity` implements `Persistable<UUID>` and carries a `@Transient isNew` flag that drives the INSERT-vs-UPDATE choice for assigned ids: `create` sets it `true`, `update` loads the current row first (absent → `404`, no save; also the source of the immutable-contentType check) and sets it `false`.
3. **Elasticsearch indexing** — `ContentItemIndexer` picks the *first* matching `ESRecordTransformer` bean (e.g. `BlogTransformer`), which maps one content item to **one or more** `EntityAsMap` ES documents. No match = warning logged, nothing indexed (the Postgres save still succeeds).

Both transformer families are discovered by Spring `List<T>` injection of `@Component` beans and gated by `Predicate.test()` — to support a new content type, add a new transformer bean of either kind; no registration step exists.

`ContentItemEntity` is purely the Spring Data persistence model — no Jackson annotations, since it is neither accepted nor returned over HTTP (the DTOs above carry the schemaless `@JsonAnySetter`/`@JsonAnyGetter` instead). It is mostly schemaless: an `@Id` and a first-class `contentType` (its own `content_type` column), plus a `SchemalessData` wrapper around a `Map<String, Object>` of everything else. `contentType` is an explicit field, passed to the `(id, contentType, map)` constructor. Spring Data instantiates loaded rows via the `@PersistenceCreator fromDatabase` static factory, which marks them not-new.

Other entry points:
- `POST /search/withcontent` (`SearchController`) — accepts a raw Elasticsearch query JSON body, runs it against the index, then hydrates full content items from Postgres by the ids found in the hits, mapping them to `ContentItemResponseDTO`s (`SearchResultsWithContent` carries both the raw ES response and the DTOs; it has custom Jackson serializers in the `elasticsearch` package).
- `PUT /index/create` (`IndexController`) — creates the ES index using `src/main/resources/elasticsearch/mappings.json`. Index name and mappings file come from `elasticsearch.index.*` in `application.yaml`; the index name is injected app-wide as a single `IndexCoordinates` bean (`ElasticSearchConfig`).

Swagger UI is available via springdoc at `/swagger-ui/index.html`.

## Test Conventions

The authoritative test standards live in the **`spring-boot-testing` skill** (`.claude/skills/spring-boot-testing/`). In short: BDD-style `@Nested` classes following the **UnitUnderTest > Given > When > Then** `@DisplayName` nesting (Given is world state only, never method inputs; When is inputs/actions), `@DisplayName` on every test and nested class, given/when in `@BeforeAll`, one *logical* assertion per `@Test` (multiple `assertThat` statements are fine when they verify one concept or match a sample). The `per_class` test-instance lifecycle is set globally in `src/test/resources/junit-platform.properties` — this is what allows `@BeforeAll` on instance methods, so no per-class annotation is needed. Solitary unit tests are named `*Test` (Surefire); Testcontainers integration tests are named `*IT` (Failsafe).

Container setup is shared through `testutils.PostgresContainerUtils` / `ElasticSearchContainerUtils`: declare a `@Container static` field and register its URI in a `@DynamicPropertySource` method. Controller integration tests extend `AbstractContentItemControllerIT` for a `WebTestClient` bound to the content items endpoint (spring-webflux is a test-only dependency for this; replacing it with `RestClient`/`MockMvc` is a deferred roadmap item-1 follow-up), and `@MockitoBean` the `ContentItemIndexer` when ES isn't under test (stub helpers in `StubbingUtils`).
