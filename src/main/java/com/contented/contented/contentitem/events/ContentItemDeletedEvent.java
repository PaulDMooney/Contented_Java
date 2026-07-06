package com.contented.contented.contentitem.events;

import java.util.UUID;

/**
 * Raised when all versions of a logical content have been deleted. Handled after the delete
 * transaction commits so the Elasticsearch document is removed only once the database delete is durable.
 */
public record ContentItemDeletedEvent(UUID identifier) {
}
