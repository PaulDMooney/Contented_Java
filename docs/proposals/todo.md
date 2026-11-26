# Proposals: still to design

Design work the proposals in this folder depend on but don't yet cover. Each entry says what's
already settled and what the design has to answer.

## 1. Elasticsearch

**Settled** ([content-model-redesign.md §3](content-model-redesign.md#indexing-and-search)): only
live versions are indexed, never drafts. Indexing runs off domain events after the transaction
commits. Each content type's outbound hook maps a live version to one or more search documents.
Search is exposed as `POST /search/withcontent`, whose details are open
([api-proposal.md §6](api-proposal.md#6-search)).

**To answer:**

- How documents are keyed now that a content item has one live version per language, and which
  system fields every document carries.
- How a query that accepts several languages avoids returning the same content once per
  language: collapsing at query time, or shaping one document per language at index time.
- How deleting a variant or a content item removes every document its hooks produced.
- Whether, and how, front-end-specific documents are produced, and how they relate to the
  shared base layer's merger
  ([content-model-redesign.md §4](content-model-redesign.md#where-the-merge-runs)).
- What the search endpoint accepts and returns: a raw query passed through, or a query shape of
  our own.
- How an index is rebuilt on this model (see [index-rebuild-design.md](../index-rebuild-design.md)).

## 2. Multi-datacenter scalability

**Settled:** Postgres is the system of record and Elasticsearch is derived from it. All ids are
server-generated UUIDv7, so any node in any region can mint one without coordination.

**To answer:**

- **Writes.** Whether there is a single primary region for writes, or writes are accepted in
  several regions. Multi-region writes raise conflicts on the one mutable row, the working draft,
  and on publish moving a variant's live pointer.
- **Reads.** Whether delivery reads are served from regional replicas, and how stale a replica
  is allowed to be after a publish.
- **Search.** Whether each region has its own Elasticsearch cluster fed from its own database
  replica, or indexes are replicated across clusters.
- **Events.** How the after-commit events that drive indexing reach other regions reliably, for
  example through an outbox table rather than in-process listeners.
- **Files.** How binary storage and file metadata are replicated, once their design exists
  ([content-model-redesign.md §5](content-model-redesign.md#5-files)).
