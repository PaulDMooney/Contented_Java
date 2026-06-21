--liquibase formatted sql

--changeset contented:001-create-content-item
CREATE TABLE content_item (
    id           uuid PRIMARY KEY,
    content_type text NOT NULL,
    data         jsonb NOT NULL
);
--rollback DROP TABLE content_item;
