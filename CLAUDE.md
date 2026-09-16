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

Ids are **intrinsic** (server-generated UUIDv7, assigned in `ContentItemService`); clients never supply them. Content is **versioned**: every content item goes through a `WORKING` (mutable draft) → `LIVE` (immutable, published) → `ARCHIVED` (immutable, history) lifecycle. A logical content is the set of rows sharing a version-agnostic `identifier`; each version additionally has its own `versionId` (the row's primary key). URLs mirror the split — content-level operations key off `identifier`, version-level operations key off the globally unique `versionId`. Full data-model and API diagrams live in [docs/content-model.md](docs/content-model.md); the summary:

- `POST /contentitems` (`createContentItem` → `ContentItemService.create`) — creates a new `WORKING` draft (`201` + `Location`). Nothing is live until an explicit publish, so drafts are not indexed.
- `PUT /contentitems/{identifier}` (`updateWorking` → `ContentItemService.updateWorking`) — updates the `WORKING` draft in place, or creates one lazily if the content currently has none (e.g. it was just published). `404` if the identifier is unknown.
- `POST /contentitems/{identifier}/publish` (`publish`) — promotes `WORKING` → `LIVE` and demotes the previous `LIVE` → `ARCHIVED`, both in one transaction. `404` if the identifier is unknown; `400` if there is no working draft to publish.
- `GET /contentitems/{identifier}` (`getContentState` → `ContentItemService.getContentState`) — returns a `ContentItemWorkAndLiveDTO` envelope (`{ working, live }`, either may be `null`).
- `GET /contentitems/{identifier}/versions` — version history summaries (no content), newest first.
- `GET /contentitems/versions/{versionId}` / `POST /contentitems/versions/{versionId}/restore` — read one version's full content, or copy it into the `WORKING` draft (history is never modified).
- `DELETE /contentitems/{identifier}` — removes every version of the content (`204`).

The persistence entity never leaves the service. `ContentItemService` accepts a `ContentItemDTO` and returns `ContentItemResponseDTO`s (reads too); `ContentItemEntity` is created, saved, and mapped back entirely inside the service via `ContentItemMapper` (`toEntity`/`toResponse`/`toSummary`). The controller only deserializes the request into a `ContentItemDTO` and forwards to the service — it never touches an entity or a repository. The request `ContentItemDTO` and response `ContentItemResponseDTO` share an `AbstractContentItemDTO` base holding `contentType` and the schemaless content nested under a `data` map, so fixed/system fields (`versionId`, `identifier`, `state`, `versionCreatedDatetime` — response-only) stay flat at the top level and can never collide with or be overridden by client content.

`contentType` is required on both writes — a create or update with a blank `contentType` is rejected with `400` — and is **immutable** across every version of a content: an update whose `contentType` differs from the stored one is rejected with `400`. All `400`/`404`/`409` responses carry an RFC 9457 `application/problem+json` body (`ContentItemExceptionHandler` maps `InvalidContentItemException`, `ContentItemNotFoundException`, and `DataIntegrityViolationException` — a concurrent write losing the race on a uniqueness invariant — to `ProblemDetail`).

`create` and `updateWorking` both run the same write pipeline (note that neither indexes — see below):

1. **Inbound transformation** — `TransformationHandler` runs the entity through the *first* `ContentItemEntityTransformer` bean whose `test()` predicate matches (e.g. `StandardDMSContentTransformer`, which normalizes `language` and stamps `modDate`; it gates on `contentType`). No match = entity passes through unchanged.
2. **Postgres save** — `ContentItemRepository` (Spring Data JDBC `ListCrudRepository`). `ContentItemEntity` implements `Persistable<UUID>` and carries a `@Transient isNew` flag that drives the INSERT-vs-UPDATE choice: a brand-new version (`ContentItemEntity.newVersion`) is `isNew = true`; an in-place edit of the existing `WORKING` row (`withData`) or a state flip on a loaded row (`withState`, used by publish/restore) is `isNew = false`. The one-`LIVE`/one-`WORKING`-per-identifier invariants are enforced by partial unique indexes, not application code, so a losing concurrent write surfaces as a `409` rather than corrupting data.

**Elasticsearch indexing is decoupled from the write pipeline.** Only the **`LIVE`** version is ever indexed, keyed by `identifier` (each publish overwrites the single live document for that content); drafts are never indexed. Indexing runs off `ContentItemPublishedEvent`/`ContentItemDeletedEvent` — published by `publish`/`deleteByIdentifier` — and is consumed by `ContentItemIndexingListener` via `@TransactionalEventListener(phase = AFTER_COMMIT)`, so a rolled-back write can never leave a stale ES document. `ContentItemIndexer` picks the *first* matching `ESRecordTransformer` bean (e.g. `BlogTransformer`), which maps one content item to **one or more** `EntityAsMap` ES documents; no match = warning logged, nothing indexed.

Both transformer families are discovered by Spring `List<T>` injection of `@Component` beans and gated by `Predicate.test()` — to support a new content type, add a new transformer bean of either kind; no registration step exists.

`ContentItemEntity` is purely the Spring Data persistence model — no Jackson annotations, since it is neither accepted nor returned over HTTP (the DTOs above carry the schemaless content instead). Its fields: `@Id versionId` (the row's PK), `identifier` (version-agnostic, shared across a content's versions), a first-class `contentType`, `state` (`ContentItemState`: `WORKING`/`LIVE`/`ARCHIVED`), an immutable `versionCreatedDatetime`, and a `SchemalessData` wrapper around a `Map<String, Object>` of everything else. The entity is itself immutable — built via `@Builder(toBuilder = true)` on a private all-args constructor, with `@With` on `state` for the publish/restore state flips and a `withData` method for in-place `WORKING` edits — rather than exposing setters. Spring Data instantiates loaded rows via the `@PersistenceCreator fromDatabase` static factory, which marks them not-new.

Other entry points:
- `POST /search/withcontent` (`SearchController`) — accepts a raw Elasticsearch query JSON body, runs it against the index, then hydrates full content items from Postgres by the `versionId`s found in the hits, mapping them to `ContentItemResponseDTO`s (`SearchResultsWithContent` carries both the raw ES response and the DTOs; it has custom Jackson serializers in the `elasticsearch` package). Since only `LIVE` versions are indexed, results are always live content.
- `PUT /index/create` (`IndexController`) — creates the ES index using `src/main/resources/elasticsearch/mappings.json`. Index name and mappings file come from `elasticsearch.index.*` in `application.yaml`; the index name is injected app-wide as a single `IndexCoordinates` bean (`ElasticSearchConfig`).

Swagger UI is available via springdoc at `/swagger-ui/index.html`.

## Test Conventions

The authoritative test standards live in the **`spring-boot-testing` skill** (`.claude/skills/spring-boot-testing/`). In short: BDD-style `@Nested` classes following the **UnitUnderTest > Given > When > Then** `@DisplayName` nesting (Given is world state only, never method inputs; When is inputs/actions), `@DisplayName` on every test and nested class, given/when in `@BeforeAll`, one *logical* assertion per `@Test` (multiple `assertThat` statements are fine when they verify one concept or match a sample). The `per_class` test-instance lifecycle is set globally in `src/test/resources/junit-platform.properties` — this is what allows `@BeforeAll` on instance methods, so no per-class annotation is needed. Solitary unit tests are named `*Test` (Surefire); Testcontainers integration tests are named `*IT` (Failsafe).

Container setup is shared through `testutils.PostgresContainerUtils` / `ElasticSearchContainerUtils`: declare a `@Container static` field and register its URI in a `@DynamicPropertySource` method. Controller integration tests extend `AbstractContentItemControllerIT` for a `RestTestClient` (`org.springframework.test.web.servlet.client`) bound to the running server (`bindToServer().baseUrl(...)`) so requests go over the real network stack — no spring-webflux on the test classpath — and `@MockitoBean` the `ContentItemIndexer` when ES isn't under test (stub helpers in `StubbingUtils`).
