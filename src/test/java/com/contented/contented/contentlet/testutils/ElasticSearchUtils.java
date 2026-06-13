package com.contented.contented.contentlet.testutils;

import org.awaitility.Awaitility;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.time.Duration;
import java.util.concurrent.Callable;

public class ElasticSearchUtils {

    /***
     * Wait for whatever changes were made to be reflected in the ES index.
     *
     * Changes only become searchable after the next index refresh (~1s by default),
     * so poll the supplied condition rather than sleeping for a fixed period.
     */
    public static void waitForESToAffectChanges(Callable<Boolean> changesAreVisible) {
        Awaitility.await("Elasticsearch to reflect changes")
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(250))
            .until(changesAreVisible);
    }

    /***
     * Wait until a search by id returns the expected number of hits:
     * 1 after indexing a document, 0 after deleting one.
     */
    public static void waitForESDocumentCount(ElasticsearchOperations elasticsearchOperations,
                                              IndexCoordinates indexCoordinates,
                                              String id,
                                              long expectedCount) {
        waitForESToAffectChanges(() -> {
            var query = new CriteriaQuery(new Criteria("id").is(id));
            return elasticsearchOperations.search(query, EntityAsMap.class, indexCoordinates).getTotalHits() == expectedCount;
        });
    }
}
