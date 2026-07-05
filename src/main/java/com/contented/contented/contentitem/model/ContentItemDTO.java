package com.contented.contented.contentitem.model;

/**
 * The request body for creating/editing content: a {@code contentType} plus the schemaless
 * {@code data} map. Identity is never supplied by the client — it is server-assigned on create and
 * taken from the URL on edit — so this type carries no id.
 */
public class ContentItemDTO extends AbstractContentItemDTO {
}
