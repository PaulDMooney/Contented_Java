package com.contented.contented.contentitem.events;

import com.contented.contented.contentitem.model.ContentItemEntity;

/**
 * Raised when a version has been promoted to LIVE. Handled after the publish transaction commits so
 * Elasticsearch is only written once the database state is durable.
 */
public record ContentItemPublishedEvent(ContentItemEntity liveVersion) {
}
