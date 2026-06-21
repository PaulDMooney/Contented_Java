# Design Ideas / Roadmap

Planned design changes for this repository. Each item links to a detailed design doc where
one exists; the rest capture intent, known considerations, and open questions so they can be
fleshed out before implementation.

| # | Initiative | Status | Design doc |
|---|---|---|---|
| 1 | Convert reactive → non-reactive (virtual threads) | Mostly done | — |
| 2 | Micrometer trace ids in logs | Mostly done | — |
| 3 | MongoDB → Postgres migration | Mostly done | — |
| 4 | Bring tests up to new testing standards | In progress | `spring-boot-testing` skill |
| 5 | Content versioning (live / working / history) | Idea | — |
| 6 | Content grouping (language variants) | Idea | — |
| 7 | Restructure into libraries / modulith | Idea | — |
| 8 | Index rebuild mechanism | Designed | [index-rebuild-design.md](index-rebuild-design.md) |
| 9 | Clarify web/service/persistence boundary (commands + response DTOs) | Done | — |

## 1. Convert from reactive to non-reactive (virtual threads)

Replace the WebFlux/Reactor stack with blocking Spring MVC-style code on the latest Spring
Boot and latest Java LTS, with virtual threads enabled (`spring.threads.virtual.enabled=true`).

**Rationale**: with virtual threads, blocking a thread on I/O is cheap, which removes the main
throughput argument for reactive. The remaining reactive advantage (backpressure-heavy
streaming) is not something this application does. Blocking code is simpler to write, read,
debug, and test — and it simplifies upcoming work (notably the index rebuild job, which
becomes a plain loop).

**Scope**:

- ~~`spring-boot-starter-webflux` → `spring-boot-starter-web`; controllers return plain
  values instead of `Mono`/`Flux`.~~ ✅ Done (`feat/non-reactive-mvc`), virtual threads enabled.
- ~~Reactive repositories → blocking ones (`ReactiveCrudRepository` → `CrudRepository`,
  `ReactiveElasticsearchOperations`/`ReactiveElasticsearchClient` → blocking equivalents).~~ ✅ Done.
- ~~Remove `Hooks.enableAutomaticContextPropagation()` (Reactor-specific; see item 2).~~ ✅ Done.
- ~~`StepVerifier` is gone; `WebTestClient` test plumbing → blocking equivalent.~~ ✅ Done
  (with item 4): controller integration tests use `RestTestClient` (Spring Framework 7) bound to
  the running server, so the test-only `spring-boot-starter-webflux` dependency is gone.
- ~~Upgrade Spring Boot and Java to latest (Boot 3.2.5 / Java 21 today; move to current LTS)~~
  ✅ Done (`feat/java-25`): Boot 3.5.14 / Java 25, Elasticsearch server 8.18 to match the
  client the Boot parent manages. Boot 4.x (Spring Framework 7) deliberately deferred.
- ~~Boot 4.x~~ ✅ Done (`chore/upgrade_to_spring_4_1_0`): Boot 4.1.0 / Spring Framework 7,
  Jackson 2 → 3 (`tools.jackson`; the ES `SearchResponse` serializers now use the client's
  `Jackson3Jsonp*` bridge), `@MockBean` → `@MockitoBean`, starter renamed
  `web` → `webmvc`, Testcontainers 2.x (`testcontainers-*` artifact ids),
  `spring.data.mongodb.uri` → `spring.mongodb.uri`, springdoc 3.x,
  Elasticsearch server/client 9.4.2.

**Notes**: keep R2DBC out of consideration for the potential Postgres move — plain JDBC on
virtual threads is the point of this conversion.

## 2. Micrometer trace ids in logs

Ensure every log line carries trace/span ids via Micrometer Tracing.

**Current state** (`feat/log4j2-trace-ids`): HTTP-request-scoped logs carry trace/span ids,
verified against the integration tests (all log lines of one `PUT /contentitems` save flow —
Mongo save + ES indexing — share a trace id). The logging backend is now **Log4j2**
(`spring-boot-starter-log4j2`; `spring-boot-starter-logging`/Logback excluded everywhere,
including test scope), which matches the Lombok `@Log4j2` loggers the code already used.
Correlation comes from Spring Boot 3.2+'s built-in correlation pattern (automatic when
`micrometer-tracing-bridge-brave` is on the classpath); the old custom
`logging.pattern.level` was removed — its `%X{traceId:-}` default-value syntax was
Logback-specific and rendered empty on Log4j2.

**Remaining work**: explicitly start/scope an observation for background work (e.g. the index
rebuild job in item 8, so all logs from one rebuild share a trace id — do this when item 8 is
built). Trace export (Zipkin/OTLP) deliberately not set up: log correlation alone is the goal
for now; no exporter dependency exists, so spans are created for correlation but go nowhere.

## 3. MongoDB → Postgres migration

Replace MongoDB with Postgres as the system of record.

**Scope**:

- **Schema**: the schemaless content item maps naturally to a thin relational shell around a
  JSON document — `id uuid PRIMARY KEY, data jsonb` (plus promoted columns later as items 5/6
  give fields real meaning). `ContentItemEntity`'s `@JsonAnySetter` shape can stay.
- **Access technology**: **Spring Data JDBC** (decided) — the repository surface is simple
  CRUD + the keyset query, so it is sufficient and the least magic. The schemaless map maps to
  a single `jsonb` column via a `JdbcCustomConversions` converter on a `SchemalessData` wrapper
  type (the per-property `@ValueConverter` path is MongoDB/Cassandra-only, not supported for JDBC).
- **Schema migrations**: **Liquibase** (decided), formatted-SQL changelogs — first time the
  project has a real schema to version.
- **Ids**: native `uuid` column; adopt UUIDv7 for new ids (rationale in
  [index-rebuild-design.md](index-rebuild-design.md) → Database considerations).
- **Data migration**: corpus is tens of thousands of records — a one-off batch copy (keyset
  walk over MongoDB, insert into Postgres) is enough; no need for sustained dual-store
  operation beyond a short cutover window.
- **Infrastructure**: docker-compose `mongo`/`mongo-express` → `postgres` (+ pgAdmin if
  desired); Testcontainers MongoDB module → Postgres module in the test utilities.

**What gets easier afterwards**: real transactions (e.g. the atomic DB-delete +
deletion-log insert in the rebuild's Option B becomes trivial), and relational constraints
for the versioning/grouping identity model (items 5/6). `FOR UPDATE SKIP LOCKED` also becomes
available if job claiming ever needs it.

**Open questions**: query patterns against `data` (does anything need a GIN index on the
jsonb?); whether the ES "with content" hydration path changes at all (it shouldn't — it is
id-based).

## 4. Testing standards

Bring the test suite up to the standards defined in the **`spring-boot-testing` skill**
(`.claude/skills/spring-boot-testing/`). The standards are owned by that skill — not a doc in
this repo — so the work here is to audit the existing suite against it and remediate the gaps.

**Standards in force** (see the skill for the authoritative version): the three-test-type model
(solitary `*Test` / sociable `*SociableTest` / integration `*IT`); UnitUnderTest > Given > When >
Then `@DisplayName` nesting with Given reserved for world state (never method inputs); one logical
assertion per `@Test`; AssertJ; `@MockitoBean`/`@MockitoSpyBean`; Testcontainers for real
infrastructure; Instancio for randomized data; the global `per_class` lifecycle via
`junit-platform.properties`; spy-verification display names that name the method under test.

**Done so far**:

- Global `per_class` lifecycle set in `src/test/resources/junit-platform.properties`; the custom
  `@NestedPerClass` annotation and all longhand `@TestInstance(PER_CLASS)` removed in favour of
  plain `@Nested`.
- `@DisplayName` voice/casing normalised to the skill's `It should …` / `When …` / `Given …` forms;
  `Then …`- and `the X should …`-style leaf names rewritten.
- `ContentItemServiceTest` re-nested so create scenarios sit under a `create` context as `When …`
  cases; spy-verification names now reference `ContentItemRepository#save`.
- Scratch `NestedDiscoveryTest` deleted; the smoke test got a `@DisplayName`.
- **Surefire→Failsafe split**: solitary unit tests are `*Test` (Surefire, `test` phase, no Docker);
  Testcontainers integration tests renamed to `*IT` (Failsafe, `integration-test`/`verify`). The
  `maven-failsafe-plugin` is wired in `pom.xml`; CI runs `mvn verify` then generates both surefire
  and failsafe HTML reports. `IntegrationTest` tags are kept as metadata.

**Carried-over conventions** worth keeping: `@MockitoBean`-ing the indexer when ES is not under
test; the `@NoDatabase` annotation (introduced with item 3) that excludes the JDBC/Liquibase
auto-config so DB-free tests start no Postgres container — generalise this (e.g. a
`@NoElasticsearch` counterpart) as part of this work.

The `WebTestClient` → `RestTestClient` migration (dropping the test-only spring-webflux dependency,
shared with item 1) is **done** — controller integration tests now go over the real network stack
via `RestTestClient.bindToServer()`.

**Still open**: integration tests share DB/container state across siblings with no `@AfterAll`
cleanup (sibling-bleed risk); the exploratory `ElasticSearchDiscoveryIT` tests ES itself rather than
the app (keep as labelled exploratory, or prune?); Instancio is unused; unit vs integration coverage
targets and whether to enforce a JaCoCo floor.

## 5. Content versioning: live / working / history

Each piece of content has a **working** version (draft, editable) and a **live** version
(published, what search/delivery serves), plus a retained **history** of previous live
versions.

**Use cases**: edit without affecting the published version; publish = promote working →
live; audit/rollback via history.

**Core principle — immutability**: the live version and all historical live versions are
immutable; the **working version is the only mutable record**. Every edit goes to working,
and publishing creates a new live version rather than modifying the current one (the old
live moves to history as-is). This keeps history trustworthy for audit/rollback, makes
"what was live at time T" answerable, and simplifies caching and ES indexing (a live
version's content never changes under its `inode` — only which version *is* live changes).

**Key design questions to resolve**:

- **Data model**: single collection/table with a version-state discriminator
  (`LIVE | WORKING | ARCHIVED` + version number) vs separate storage for history. The
  legacy-DMS field vocabulary already in `StandardDMSContentTransformer` is suggestive:
  `inode` (version-specific id) vs `identifier` (version-agnostic id) — versioning would give
  these real meaning: one `identifier` ↔ many `inode`s, of which one is live and one is working.
- **Operations**: save-to-working, publish (working → live, old live → history), unpublish,
  discard working, restore from history (copies the historical version into a new working
  version — history itself is never modified). What does today's `PUT /contentitems` map to —
  save-and-publish in one step?
- **Indexing**: presumably only the **live** version is indexed for delivery search; decide
  whether working versions get indexed separately (e.g. a `live` flag on ES docs — the
  `LIVE_FIELD` already exists — or a separate working index) for editorial search.
- **History retention**: unbounded vs capped (N versions / age-based).
- **Interaction with item 8**: the rebuild walks "all content items" — under versioning this
  becomes "all live versions (+ working, if indexed)". The keyset/checkpoint design is
  unaffected, but the batch query and the ES transformers gain version-awareness.

## 6. Content grouping (language variants)

A **group** of content items shares a common identifier; each member represents a variant of
the same logical content — primarily language variants (e.g. the same blog post in `en`,
`fr`, `de`).

**Key design questions to resolve**:

- **Group identity**: this is plausibly the same `identifier` concept as in item 5 — one
  logical content, many language variants, each variant having its own working/live/history
  chain. Decide deliberately whether group id and version-agnostic id are one concept or two
  (the legacy DMS model they echo treats `identifier` + `language` as the variant key).
- **API shape**: fetching by group identifier **returns all content items in that group**
  (e.g. `GET /contentitems/group/{identifier}` → every language variant). Beyond that:
  possibly a convenience fetch for a single best-match variant by language with fallback
  rules (requested language → default language?), and creating a variant of an existing
  group.
- **Constraints**: at most one variant per language per group; what happens on delete — does
  deleting the last variant delete the group?
- **Indexing/search**: search should likely be language-filterable, and "with content"
  hydration (see `SearchController`) may want to return the whole group or the best variant.
  The `language` field is already normalized by `StandardDMSContentTransformer`.
- **Interaction with item 5**: versioning is per-variant, not per-group (publishing the French
  version must not publish the English one). Designing items 5 and 6 together is strongly
  recommended — they share the identity model.

## 7. Restructure into libraries / modulith

Separate **core** functionality (persistence, ES indexing, generic REST endpoints, rebuild
job) from **content-type-specific** functionality (e.g. Blog transformers), so new content
types can be added without touching core.

**Options**:

- **Spring Modulith**: single deployable, enforced module boundaries
  (`core`, `contenttype-blog`, …), with verification tests that fail on illegal cross-module
  dependencies. Lowest ceremony; keeps the current single-app deployment.
- **Multi-module Maven build**: separate JARs (`contented-core`, `contented-blog`, an
  application assembly module). Heavier, but allows content-type modules to be versioned and
  shipped independently later.

**Notes**: the existing extension seams already point the right way — content types plug in
via `ContentItemEntityTransformer` / `ESRecordTransformer` beans discovered by `List<T>`
injection, so the Blog-specific code (`BlogTransformer`, the Blog parts of
`StandardDMSContentTransformer.SUPPORTED_TYPES`) should extract cleanly. The current
package-by-feature layout (`contentitem`, `contentitem.elasticsearch`, …) maps naturally onto
modules. Start with Spring Modulith; promote to multi-module Maven only if independent
shipping becomes a real need.

## 8. Index rebuild mechanism

Implement the alias-swap rebuild designed in
**[index-rebuild-design.md](index-rebuild-design.md)**: generated-name indices behind a
stable alias; a cancellable/resumable checkpointed batch job (keyset pagination, job record
in the DB, heartbeat takeover, progress %); naive dual-write during the rebuild with a
catch-up pass + deletion log finalization (Option B); atomic alias swap with rollback window.

## 9. Clarify the web/service/persistence boundary (commands + response DTOs)

**Status: Done.** `ContentItemEntity` is now purely the Spring Data persistence model (no Jackson)
and never leaves the service. `ContentItemService` accepts a `ContentItemDTO` and returns a
`ContentItemResponseDTO` (reads included), mapping entity ↔ DTO internally via `ContentItemMapper`;
the service owns domain validation. The controller only does the transport-level id checks and
forwards the DTO — it no longer touches an entity or a repository. `SearchController` gets response
DTOs straight from the service. Request and response DTOs share an `AbstractContentItemDTO` base.

Keep `ContentItemEntity` off the HTTP boundary so the service owns domain validation and entity
construction and the controller is a thin HTTP adapter. Previously `ContentItemController` built a
persistence-coupled `ContentItemEntity` on the way in and returned the raw entity on the way out,
and write-path validation was split between the two layers.

**Problem**:

- `ContentItemEntity` is doing the job of four models at once — JSON request target, JSON response
  body, domain model, and persistence row — carrying both Jackson (`@JsonAnySetter`/`@JsonAnyGetter`)
  and Spring Data (`@Table`, `@Id`, `@PersistenceCreator`, `Persistable`, `isNew`) annotations. So
  the web layer instantiates a persistence-framework object and reasons about persistence lifecycle
  (null id ⇒ "please generate"; the `isNew` flag).
- Write-path validation is split: request-shape rules (no id on POST, body-id vs URL-id on PUT)
  sit in the controller; domain rules (contentType required/immutable) sit in the service only
  because they need a DB read. The intrinsic-id rule is half in each layer.

**Approach**: keep `ContentItemEntity` off the HTTP boundary, and out of the service's public API,
in both directions.

- *Inbound*: the service accepts a `ContentItemDTO` and constructs the `ContentItemEntity` internally
  (via `ContentItemMapper`), owning all write-path validation. (An earlier iteration routed an
  explicit command object between controller and service; see Resolved decisions for why it was
  dropped.)
- *Outbound*: the service returns a `ContentItemResponseDTO`, never the raw `ContentItemEntity`.

With both in place, `ContentItemEntity` no longer needs its Jackson annotations and becomes purely
the Spring Data persistence model.

**Scope**:

- `ContentItemService.create`/`update` accept a `ContentItemDTO` and return a `ContentItemResponseDTO`;
  a `ContentItemMapper` constructs and reads back the `ContentItemEntity`.
- Controller stops exposing `ContentItemEntity` (and stops touching the repository); read endpoints
  return response DTOs too.
- Strip Jackson annotations from `ContentItemEntity` once it is neither accepted nor returned over
  HTTP.
- Move entity construction and the intrinsic-id "no client id" rule out of the controller.
- Decide per-check whether it is a transport concern (stays in the controller — e.g. reconciling a
  body id against the URL id) or a domain rule (centralised in the service alongside contentType
  validation).

**Resolved decisions**:

- The service speaks DTOs in and out rather than taking explicit primitive parameters or dedicated
  command objects. Command records were prototyped but dropped — they were anemic (no behaviour
  beyond decomposing the DTO), so they added a type without earning its keep. The (mild,
  widely-accepted) cost is that the service references a Jackson-annotated type; not worth a third
  type to avoid.
- A dedicated `ContentItemResponseDTO` rather than reusing `ContentItemDTO`, with a shared
  `AbstractContentItemDTO` base for the common shape (`id`, `contentType`, schemaless any-getter/
  any-setter map). Distinct types keep request/response free to diverge later.
- Entity ↔ DTO mapping lives in a hand-written `ContentItemMapper` `@Component` (not MapStruct or a
  Spring `Converter`): the schemaless map defeats MapStruct's codegen advantage, and there is only
  one entity. Revisit if the type/mapping count grows (items 5/6).
- The id-reconciliation checks (no id on POST, body-id-vs-URL-id on PUT) stayed in the controller
  as transport concerns — the URL only exists at the HTTP layer. The domain rules (contentType
  required/immutable) stayed in the service.
- Tests that deserialized responses into `ContentItemEntity` now deserialize into
  `ContentItemResponseDTO`; the deeper `WebTestClient` → `RestTestClient` migration landed with item 4.

**Why standalone/now**: independent of the domain features, but items 5 (versioning) and 6
(grouping) add operations (publish, save-to-working, create-variant) that each want a clean,
intent-revealing service input — establishing the command boundary first avoids retrofitting it
across many new operations.

## Sequencing

```mermaid
flowchart LR
    I1["1 - Non-reactive +<br/>virtual threads"] --> I2["2 - Trace ids in logs"]
    I1 --> I4["4 - Testing standards"]
    I1 --> I3["3 - Postgres migration"]
    I3 --> I8["8 - Index rebuild"]
    I7["7 - Modulith restructure"] --> I5["5 - Versioning"]
    I5 <--> I6["6 - Grouping /<br/>language variants"]
    I5 --> IR["(revisit rebuild queries<br/>+ transformers)"]
    I8 --> IR
    I9["9 - Web/service boundary<br/>(commands + response DTOs)"]
```

Suggested order and reasoning:

1. **Item 1 first** — it is the foundation: the Postgres migration wants the blocking JDBC
   stack, the rebuild design assumes blocking code, trace propagation is simpler without
   Reactor, and tests get rewritten anyway, so doing it first avoids double work.
2. **Items 2 and 4 ride along** with the conversion (log verification and test rewrites are
   part of touching everything once).
3. **Item 3 (Postgres) next** — before the rebuild and the domain features, so the job
   record, deletion log, and keyset queries are built once against the final database, and
   versioning/grouping can lean on relational constraints and transactions from the start.
4. **Item 8 after the migration** — its design is done and deliberately DB-agnostic, but
   implementing it once on Postgres beats implementing on MongoDB and re-testing after a
   store swap.
5. **Item 7 before items 5 and 6** — versioning and grouping are the largest domain changes;
   better to land them inside clean module boundaries than to untangle modules afterwards.
6. **Items 5 and 6 designed together** (shared identity model: `identifier` / `language` /
   `inode`), then implemented in either order. Afterwards, revisit the rebuild job's batch
   query and ES transformers for version/variant awareness.
7. **Item 9 is independent and near-term** — it can land before the domain features and has no
   hard dependency on them, but doing it first gives items 5/6/7 a clean service-input boundary
   to build their new operations on.
