package com.contented.contented.contentitem.elasticsearch;

import com.contented.contented.contentitem.events.ContentItemDeletedEvent;
import com.contented.contented.contentitem.events.ContentItemPublishedEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps Elasticsearch in step with the database, but only <em>after</em> the originating transaction
 * commits. Indexing inside the transaction risked ES holding a live document for a version Postgres
 * never actually promoted (or losing the ES write if the commit later failed); running it after
 * commit makes the database the single source of truth and ES an eventually-consistent follower.
 */
@Log4j2
@Component
public class ContentItemIndexingListener {

    private final ContentItemIndexer contentItemIndexer;

    public ContentItemIndexingListener(ContentItemIndexer contentItemIndexer) {
        this.contentItemIndexer = contentItemIndexer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublished(ContentItemPublishedEvent event) {
        var indexed = contentItemIndexer.indexContentItem(event.liveVersion());
        log.info("Indexed `{}` documents for contentItem identifier: `{}` successfully",
            indexed == null ? 0 : indexed.size(), event.liveVersion().getIdentifier());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(ContentItemDeletedEvent event) {
        contentItemIndexer.deleteRecord(event.identifier().toString());
        log.info("Deleted ES document for contentItem identifier: `{}`", event.identifier());
    }
}
