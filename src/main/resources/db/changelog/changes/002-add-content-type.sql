--liquibase formatted sql

--changeset contented:002-add-content-type
ALTER TABLE contentlet ADD COLUMN content_type text;
UPDATE contentlet SET content_type = data ->> 'contentType';
UPDATE contentlet SET data = data - 'contentType';
ALTER TABLE contentlet ALTER COLUMN content_type SET NOT NULL;
--rollback ALTER TABLE contentlet DROP COLUMN content_type;
