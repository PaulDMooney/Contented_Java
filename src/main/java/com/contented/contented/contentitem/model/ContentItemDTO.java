package com.contented.contented.contentitem.model;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * The request body for creating/editing content: a {@code contentType} plus the schemaless
 * {@code data} map. Identity is never supplied by the client — it is server-assigned on create and
 * taken from the URL on edit — so this type carries no id. Jackson deserializes it through the
 * generated builder, so it is immutable and exposes no setters.
 */
@SuperBuilder
@Jacksonized
public class ContentItemDTO extends AbstractContentItemDTO {
}
