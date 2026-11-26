# Proposals: still to design

Design work the proposals in this folder depend on but don't yet cover. Each entry says what's
already settled, if anything, and the questions the design has to answer.

## 1. Elasticsearch

**Settled:** Postgres is the system of record; Elasticsearch is derived from it and can always be
rebuilt from it. Each content type supplies hooks that turn a version into one or more search
documents. Search is exposed as `POST /search/withcontent`, with its details open
([api-proposal.md §6](api-proposal.md#6-search)).

**Requirement:** the index is kept up to date automatically as content is created, updated,
published, unpublished and deleted — **and the same code can rebuild a whole index from
scratch.** There is one way to turn content into search documents, not one for live updates and
another for rebuilds; a rebuild is that same code run over every content item instead of one.

**To answer:**

- **One indexing path for incremental updates and full rebuilds.** What the shared unit of work
  is — for example "index this content item", given its current state in Postgres, which a
  content event triggers for one item and a rebuild runs for all of them. Building documents from
  the current database state, rather than from what an event carried, is what lets a rebuild
  reuse the code unchanged. Also: how a rebuild runs without taking search offline (building a
  new index behind an alias and swapping, as in
  [index-rebuild-design.md](../index-rebuild-design.md)), and how changes made while a rebuild is
  running end up in the new index.
- **When to create, read, update and delete search records.** Which content events write to the
  index (create, draft edit, publish, delete), whether indexing happens after the database
  transaction commits or through some other mechanism, and when reads go to Elasticsearch versus
  Postgres.
- **Live versus not live.** Whether only live versions are indexed, or drafts too, and if both,
  how a document says which it is and how a query is kept from mixing them. Related: a content
  item now has one live version per language, so how documents are keyed per variant, and how a
  query that accepts several languages avoids returning the same content once per language.
- **Multiple indices for different purposes.** Whether authoring, working/preview and live each
  get their own index — with their own mappings, document shapes and update triggers — or share
  one index distinguished by a field. This interacts with the push-publishing question in
  [§2](#2-multi-datacenter-scalability): if live content is served from separate live
  instances, the authoring instance may not need a live index at all.

## 2. Multi-datacenter scalability

**Settled:** All ids are server-generated UUIDv7, so any node in any region can mint one without
coordination.

**To answer:**

- **Database replication.** Whether Postgres is replicated across datacenters, and if so whether
  there is a single writable primary with read replicas, or writes in several regions. Writes in
  several regions raise conflicts on the one mutable row, the working draft, and on publish moving
  a variant's live pointer.
- **Elasticsearch replication.** Whether indices are replicated across clusters (for example
  with cross-cluster replication), or each datacenter builds its own index from its own database
  replica.
- **Push publishing.** Whether content originates in one *authoring* instance and publishing
  pushes it to separate *live* instances that serve delivery, as dotCMS does. This decides:
  - whether live instances hold only live versions, and so whether a working index is needed
    anywhere other than authoring;
  - what is pushed (versions, rendered documents, or search documents) and how a live instance
    acknowledges or rejects a push;
  - how unpublish and delete reach the live instances.

## 3. Hooks and ordering

**Settled:** content-type-specific behaviour is supplied by Spring beans selected by content type,
with no registration step. Today that is inbound transformers on save, outbound transformers for
indexing, and the base layer's merger.

**To answer:**

- **Phases or handlers.** Whether the core defines named phases that any content type can hook
  into (for example validate, transform, save, publish, index), each with a defined order — or
  each content type supplies one handler with before/after callbacks around save, publish and
  delete. Either way: how several hooks on the same phase are ordered, and what a hook may change
  or veto.
- **Events.** What the system broadcasts — created, draft updated, published, unpublished,
  deleted, per variant or per content item — what each event carries (ids only, or the version),
  and whether events are delivered in-process after commit or durably, for example through an
  outbox table, so that indexing and other subscribers can't miss one.

## 4. External content

Content whose system of record is somewhere else — a product catalogue, a people directory —
brought in so it can be searched, related to, or delivered alongside native content.

**To answer:**

- **Natural ids.** How a content item keeps the id it has in its originating system. Options
  include storing the external id beside the server-generated `identifier` with a uniqueness
  constraint on (source, external id), or letting the external id become the identifier. Either
  way: how imports and re-imports find the existing content item, and what happens if the source
  reuses or changes an id.

## 5. Files

**Settled** ([content-model-redesign.md §5](content-model-redesign.md#5-files)): binaries live in
their own storage system, never in Postgres. File metadata is its own concept with its own store
and lifecycle, separate from content items. Content items relate to file metadata explicitly,
not through opaque URLs inside `data`.

**To answer:**

- **Versioning.** Whether a live or archived version can keep pointing at a file that is later
  replaced, and so whether stored objects need to be write-once to keep history trustworthy.
- **The relationship.** Whether the link between content items and file metadata is a table of
  its own, and whether it attaches per version, per variant or per content item.
- **Language.** Whether file metadata needs language variants of its own (translated alt text, a
  French PDF), or whether that belongs on the referring content.
- **Search.** What search documents carry for a referenced file, and therefore whether a file
  change has to re-index the content that references it.
- **Uploads.** The upload flow — ideally clients upload straight to storage rather than through
  the application — and how unreferenced uploads are cleaned up.
- **Deletion.** What happens to a file that content still references, and to the stored bytes
  when its metadata is deleted.
