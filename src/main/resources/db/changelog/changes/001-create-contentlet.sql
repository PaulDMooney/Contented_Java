--liquibase formatted sql

--changeset contented:001-create-contentlet
CREATE TABLE contentlet (
    id   text PRIMARY KEY,
    data jsonb NOT NULL
);
--rollback DROP TABLE contentlet;
