# MongoDB → Postgres Migration — Design

## Problem / Goal

MongoDB is the system of record for contentlets (`ContentletService` writes Mongo first, then
indexes Elasticsearch). This migration replaces MongoDB with Postgres as the system of record,
keeping the schemaless contentlet shape intact, while setting up the relational foundation that
later items lean on: real transactions for the index-rebuild job
([index-rebuild-design.md](index-rebuild-design.md), item 8) and relational constraints for the
versioning/grouping identity model (items 5/6).

Elasticsearch is unaffected — the "with content" hydration path is id-based, so as long as the
contentlet id strings are preserved byte-for-byte, ES documents keep resolving.

## Decisions at a glance

| Area | Decision | Why |
|---|---|---|
| Access technology | **Spring Data JDBC** | Surface is CRUD + one keyset query; no associations, no lazy loading, no dirty checking to earn Hibernate's keep. Least magic for a system of record. |
| JSON mapping | `JdbcCustomConversions` converter on a **`SchemalessData`** wrapper type → `jsonb` | Per-property `@ValueConverter` is **not** the documented/supported path for Spring Data JDBC (MongoDB/Cassandra only); the global converter is. A dedicated wrapper keeps the converter unambiguous and dodges the relational module's `Map`/`Iterable` converter quirks. |
| New-entity detection | Implement **`Persistable<…>`**, drive `isNew()` from the `existsById` check already made | Spring Data JDBC treats a non-null assigned id as an UPDATE; with assigned ids that breaks inserts. We already call `existsById` for the 201/200 decision — reuse it. |
| Id column | **`uuid`** (recommended) with `text` documented as the low-friction fallback | Smaller index, native semantics, UUIDv7 locality. `text` if existing ids aren't all valid UUIDs or we want zero ripple. |
| New id generation | **UUIDv7**, app-side | Time-ordered: append to the PK B-tree's right edge, scan locality for the rebuild backfill. |
| Schema migrations | **Liquibase**, formatted-SQL changelogs | Free rollback, preconditions, contexts; formatted SQL keeps Postgres-specific DDL (`jsonb`, GIN) plain and avoids the cross-DB XML abstraction we'd never use. |
| Test infra | Testcontainers `postgresql` module | Replaces the `mongodb` module; Liquibase runs on context startup so tests get the schema for free. |

## Schema

A thin relational shell around the JSON document, as item 3 envisioned:

```sql
CREATE TABLE contentlet (
    id   uuid PRIMARY KEY,
    data jsonb NOT NULL
);
```

`ContentletEntity` stays schemaless: the `@Id` plus the schemaless map serialized into `data`.
Promoted columns (e.g. `identifier`, `language`, `version_state`) arrive with items 5/6, when
those fields gain real meaning; the `jsonb` column carries everything until then. No GIN index
yet — current access is id-based (`findById`, `findAllById`, `findAll`) plus the rebuild's keyset
scan, none of which query *into* `data`. Add a GIN index only when a real jsonb query appears.

## The entity

The persisted field becomes a `SchemalessData` wrapper; the entity's external surface (the
Jackson any-getter/any-setter and the `Map`-based constructor the transformers use) is retained,
so `StandardDMSContentTransformer`, `ContentletController`, and `ContentletDTO` are untouched.

```java
@Table("contentlet")
public class ContentletEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @JsonIgnore
    private SchemalessData data = new SchemalessData();   // wraps a LinkedHashMap

    @Transient
    private boolean isNew = true;                         // new instances are new; see below

    @Override public boolean isNew() { return isNew; }

    @JsonAnySetter public void add(String k, Object v) { data.put(k, v); }
    @JsonAnyGetter public Map<String, Object> getSchemalessData() { return data.values(); }
    // existing Map-based constructor wraps the map into SchemalessData
}
```

`SchemalessData` is a type **we create** — a thin wrapper, *not* a `Map` subclass. Subclassing
`Map` would re-trip Spring Data JDBC's collection/Map special-casing (the thing the wrapper
exists to avoid). It holds a mutable `LinkedHashMap` so `@JsonAnySetter` can keep populating it
during deserialization:

```java
public final class SchemalessData {
    private final Map<String, Object> values;
    public SchemalessData()                       { this(new LinkedHashMap<>()); }
    public SchemalessData(Map<String, Object> v)  { this.values = v; }
    public Map<String, Object> values()           { return values; }
    public void put(String k, Object v)           { values.put(k, v); }
    public <T> T get(String k)                    { return (T) values.get(k); }
}
```

## Access layer: Spring Data JDBC

`ContentletRepository` changes its base interface only:

```java
public interface ContentletRepository extends CrudRepository<ContentletEntity, UUID> {

    // keyset page for the rebuild backfill (item 8); explicit SQL is the predictable form
    @Query("SELECT * FROM contentlet WHERE id > :lastId ORDER BY id LIMIT :limit")
    List<ContentletEntity> findNextBatch(UUID lastId, int limit);
}
```

`findAll`, `findById`, `findAllById`, `existsById`, `save`, `deleteById` all carry over from
`CrudRepository`. `getAll()` in the controller keeps working (still flagged for pagination).

### jsonb converter

The whole `SchemalessData` value maps to one `jsonb` column — a single-value → single-column
conversion, exactly what the Spring Data JDBC docs say custom converters support (the unsupported
case is exploding one object across *multiple* columns, which we don't do). pgjdbc won't
implicitly cast text → jsonb, so the writing converter emits a `PGobject`:

```java
@WritingConverter
class SchemalessDataToJsonb implements Converter<SchemalessData, PGobject> {
    private final ObjectMapper mapper;                    // Jackson 3 (tools.jackson)
    public PGobject convert(SchemalessData v) {
        var o = new PGobject();
        o.setType("jsonb");
        o.setValue(mapper.writeValueAsString(v.values()));
        return o;
    }
}

@ReadingConverter
class JsonbToSchemalessData implements Converter<PGobject, SchemalessData> {
    private final ObjectMapper mapper;
    public SchemalessData convert(PGobject o) {
        return new SchemalessData(mapper.readValue(o.getValue(), MAP_TYPE));
    }
}
```

Register both by extending `AbstractJdbcConfiguration` and overriding `userConverters()` (or a
`JdbcCustomConversions` bean). Registering against `SchemalessData` rather than `Map<String,Object>`
keeps the converter from catching unrelated maps in future aggregates, and sidesteps the
relational module's known converter-application quirks around `Map`/`Iterable` types.

**`modDate` note**: `StandardDMSContentTransformer` stamps `modDate` as a `java.time.Instant`
inside the map. The ObjectMapper must serialize it consistently (ISO-8601 string) so item 8's
catch-up query — `WHERE (data->>'modDate')::timestamptz >= :startedAt` — can compare it. That
query may want an expression index later; noted as an open question, not built here.

### New-entity detection (assigned-id gotcha)

Spring Data JDBC decides INSERT vs UPDATE from whether the `@Id` is "new". With **assigned** ids
(ours come from `inode`/`dmsId`, not DB-generated), a non-null id reads as "existing" → UPDATE →
zero rows affected on a genuine insert. `ContentletService.saveToDB` already calls `existsById`
for the 201/200 decision; reuse it to drive `Persistable.isNew()`:

```java
boolean exists = contentletRepository.existsById(entity.getId());
entity.setNew(!exists);                       // INSERT when absent, UPDATE when present
var saved = contentletRepository.save(entity);
return new ResultPair(saved, !exists);
```

No extra round trip (the `existsById` was already there) and behavior is preserved (no optimistic
locking introduced). After a `findById`, entities must read as not-new — set `isNew = false` via
an `AfterConvertCallback<ContentletEntity>`.

**Alternatives considered**: a `@Version` column also resolves new-detection *and* gives
optimistic locking — attractive for item 5's mutable working version, but it changes write
semantics today (concurrent same-id saves would throw instead of last-write-wins), so deferred to
when versioning actually needs it. `JdbcAggregateTemplate.insert()/update()` is the fully explicit
escape hatch if the `Persistable` route ever feels indirect.

## Id strategy

Existing ids are legacy DMS `inode` values. The legacy DMS lineage uses UUIDs for `inode`/
`identifier`, so a native `uuid` column is the expected fit — **verify first** that every existing
id is a valid UUID (a quick scan of the Mongo `_id`s) before committing.

- **Recommended — `uuid` column, Java field `UUID`.** 16-byte index, native semantics, and
  UUIDv7 ordering locality. Ripple is localized and arguably correctness-improving: the controller
  path variable binds `String → UUID` automatically; `deriveId` parses the `inode` string to a
  `UUID` at the boundary; `ContentletIndexer` uses `id.toString()` as the ES `_id`. **Caveat**:
  `UUID.toString()` must reproduce the exact id string already stored as the ES `_id` (canonical
  lowercase hex with dashes — which matches the legacy form) or ES docs orphan. Confirm during the
  data-migration dry run.
- **Fallback — `text` column, Java field stays `String`.** Zero ripple, zero ES-match risk,
  keyset works on lexicographic order. The rebuild design is explicitly id-format-agnostic ("any
  stable total order works"), so this is a legitimate lower-risk choice; the cost is a larger
  index and forgoing native uuid semantics.

**New ids: UUIDv7**, generated app-side, for content the app mints itself (notably item 5 creating
new `inode`s/versions). Time-ordered ids append to the PK B-tree's right edge and give the rebuild
backfill scan locality. Trade-off: v7 ids reveal creation time.

## Schema migrations: Liquibase

First time the project has a real schema to version. Use **Liquibase with formatted-SQL
changelogs**, not the XML/YAML abstraction:

- The cross-DB abstraction Liquibase is famous for buys nothing against a single permanent Postgres
  target, and `jsonb`/GIN/`uuid` are cleanest as raw SQL anyway.
- Formatted SQL keeps migrations plain and readable (Flyway-like) while retaining Liquibase's
  genuine wins that *do* apply here: free built-in rollback (Flyway's automated undo is paid),
  preconditions, and contexts/labels (e.g. gate seed data to a `test` context).

```
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

Liquibase auto-runs on startup, so both the app and Testcontainers-backed tests get the schema
applied automatically.

## Data migration

The corpus is tens of thousands of records — a one-off batch copy is enough; no sustained
dual-store window. A `keyset walk over MongoDB → batch insert into Postgres` (the same keyset
shape item 8 uses) run as a one-shot `CommandLineRunner` profile or a standalone main:

1. Stand up Postgres alongside the existing Mongo, run Liquibase to create the schema.
2. Keyset-page the Mongo collection by `_id`, transforming each document to a `(uuid, jsonb)` row
   — **reuse the stored entity as-is**; do not re-run the inbound `ContentletEntityTransformer`
   (it would re-stamp `modDate`).
3. Batch-insert into Postgres.
4. Dry-run verification: row counts match, and a sample of ids round-trips so the ES `_id`
   string-match (above) holds.

ES is not touched by the migration. A full reindex (item 8) is only needed if ids change form —
another reason to preserve id strings exactly.

## Infrastructure & tests

**`pom.xml`**: drop `spring-boot-starter-data-mongodb` and `testcontainers-mongodb`; add
`spring-boot-starter-data-jdbc`, `org.postgresql:postgresql` (runtime), `org.liquibase:liquibase-core`,
and `testcontainers-postgresql` (test).

**`application.yaml`**: replace `spring.mongodb.uri` with a datasource + Liquibase block:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/demo
    username: contented
    password: example
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

**`docker-compose.yaml`**: replace `mongo`/`mongo-express` with `postgres` (port 5432, matching
credentials) and optionally `pgadmin`; leave `elasticsearch` as-is.

**Test utilities**: `MongoDBContainerUtils` → `PostgresContainerUtils`, swapping `MongoDBContainer`
for `PostgreSQLContainer` and registering `spring.datasource.url`/`username`/`password` in the
`@DynamicPropertySource`. `@Container static` + dynamic-property pattern is unchanged; Liquibase
applies the schema on context start. (Coordinate the broader test rewrite with item 4.)

## What gets easier afterwards

- **Real transactions** — item 8 Option B's "DB delete + deletion-log insert" becomes a single
  `@Transactional` unit; `FOR UPDATE SKIP LOCKED` becomes available if job claiming ever needs it.
- **Relational constraints** for items 5/6's identity model (one `identifier` ↔ many `inode`s, at
  most one variant per language) — enforceable in the DB rather than in code.

## Interactions with other roadmap items

- **Item 8 (rebuild)** sits directly on this. Build its `reindex_job` record, `deleted_contentlets`
  log, and keyset queries once, on Postgres. The keyset query shape (`WHERE id > ? ORDER BY id
  LIMIT ?`) is identical; the catch-up `modDate` query becomes a jsonb expression (above).
- **Items 5/6 (versioning/grouping)** later promote `identifier`/`language`/`version_state` out of
  `data` into real columns with constraints — the `jsonb` shell makes that an additive migration,
  not a rewrite.

## Open questions

- Do all existing legacy ids parse as valid UUIDs? (Drives the `uuid`-vs-`text` decision.)
- Does the catch-up `modDate` query need an expression index on `(data->>'modDate')`? (Defer to
  item 8 implementation.)
- Confirm the per-property `@ValueConverter` path stays unsupported/undocumented for Spring Data
  JDBC at the Boot version in use — if it becomes first-class, it would remove the need for the
  global converter and the wrapper.

## Implementation checklist

1. docker-compose: Postgres (+ pgadmin), keep ES. Verify the app's existing flows by hand.
2. Dependencies: swap Mongo → JDBC/Postgres/Liquibase; swap Testcontainers module.
3. Liquibase changelog: `contentlet (id, data)` table.
4. Entity: `SchemalessData` wrapper, `@Table`, `Persistable`, id type decision.
5. Converters + `AfterConvertCallback`; register via `AbstractJdbcConfiguration`.
6. Repository: `CrudRepository` + `findNextBatch` keyset `@Query`.
7. Service: drive `isNew()` from the existing `existsById` check.
8. Test utilities: `PostgresContainerUtils`; update `@DynamicPropertySource` registrations.
9. One-off Mongo → Postgres copy job; dry-run verification (counts + id round-trip).
10. Cutover; keep ES untouched; delete Mongo infra after a confidence window.

## Appendix A: Liquibase changelog (sketch)

A thin YAML master that `include`s versioned formatted-SQL files — each migration stays plain
SQL (the point of choosing formatted SQL), while Liquibase still gets ordered, checksum-tracked
changesets and the `--rollback` it needs for free rollback. New migrations are new files added
to the master.

`src/main/resources/db/changelog/db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-contentlet.sql
      relativeToChangelogFile: true
```

`src/main/resources/db/changelog/changes/001-create-contentlet.sql`:

```sql
--liquibase formatted sql

--changeset contented:001-create-contentlet
CREATE TABLE contentlet (
    id   uuid PRIMARY KEY,   -- text if existing ids aren't all valid UUIDs (see Id strategy)
    data jsonb NOT NULL
);
--rollback DROP TABLE contentlet;
```

No extension is needed: `uuid`/`jsonb` are native, and new ids are minted app-side (UUIDv7), so
there is no DB-side id generation to provision.

## Appendix B: `PostgresContainerUtils` (sketch)

Mirrors `MongoDBContainerUtils` exactly — static factory + start-and-register. Liquibase runs on
context start against the registered datasource, so tests get the schema with no extra wiring; the
`@Container static` + `@DynamicPropertySource` pattern in each test is unchanged, only the
registered property keys differ (`spring.datasource.*` instead of `spring.mongodb.uri`).

```java
package com.contented.contented.contentlet.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;   // Testcontainers 2.x package

public class PostgresContainerUtils {

    public static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:18-alpine")   // match docker-compose
            .withDatabaseName("demo")
            .withUsername("contented")
            .withPassword("example");
    }

    public static void startAndRegisterPostgresContainer(PostgreSQLContainer<?> container,
                                                          DynamicPropertyRegistry registry) {
        container.start();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }
}
```

