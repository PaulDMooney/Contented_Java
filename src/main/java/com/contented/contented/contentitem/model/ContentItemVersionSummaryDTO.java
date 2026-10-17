package com.contented.contented.contentitem.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A lightweight history entry: identifies a version and its state without its full content.
 */
public record ContentItemVersionSummaryDTO(UUID versionId, ContentItemState state, Instant versionCreatedDatetime) {
}
