--liquibase formatted sql

--changeset contented:002-add-content-item-versioning
-- The primary key now identifies a specific version of a logical content.
ALTER TABLE content_item RENAME COLUMN id TO version_id;
ALTER TABLE content_item ADD COLUMN identifier uuid NOT NULL;
ALTER TABLE content_item ADD COLUMN state text NOT NULL;
ALTER TABLE content_item ADD COLUMN version_created_datetime timestamptz NOT NULL;

-- At most one LIVE and one WORKING version per identifier; ARCHIVED is uncapped (history).
CREATE UNIQUE INDEX uq_content_item_live ON content_item (identifier) WHERE state = 'LIVE';
CREATE UNIQUE INDEX uq_content_item_working ON content_item (identifier) WHERE state = 'WORKING';
CREATE INDEX ix_content_item_identifier ON content_item (identifier);
--rollback DROP INDEX ix_content_item_identifier;
--rollback DROP INDEX uq_content_item_working;
--rollback DROP INDEX uq_content_item_live;
--rollback ALTER TABLE content_item DROP COLUMN version_created_datetime;
--rollback ALTER TABLE content_item DROP COLUMN state;
--rollback ALTER TABLE content_item DROP COLUMN identifier;
--rollback ALTER TABLE content_item RENAME COLUMN version_id TO id;
