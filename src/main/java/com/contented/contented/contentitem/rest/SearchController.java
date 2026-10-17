package com.contented.contented.contentitem.rest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.contented.contented.contentitem.model.ContentItemVersionResponseDTO;
import com.contented.contented.contentitem.ContentItemService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(SearchController.SEARCH_PATH)
public class SearchController {
    public static final String SEARCH_PATH = "search";

    private final ElasticsearchClient elasticsearchClient;

    private final IndexCoordinates indexCoordinates;

    private final ContentItemService contentItemService;


    public SearchController(ElasticsearchClient elasticsearchClient, IndexCoordinates indexCoordinates, ContentItemService contentItemService) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexCoordinates = indexCoordinates;
        this.contentItemService = contentItemService;
    }

    @PostMapping("/withcontent")
    public SearchResultsWithContent<EntityAsMap> searchWithContent(@RequestBody String searchRequestJSON) throws IOException {

        SearchRequest request = builderFromJSON(searchRequestJSON)
            .index(indexCoordinates.getIndexName()).build();

        SearchResponse<EntityAsMap> response = elasticsearchClient.search(request, EntityAsMap.class);

        List<UUID> extractedVersionIds = response.hits().hits().stream()
            .map(SearchController::extractVersionId)
            .filter(Objects::nonNull)
            .toList();

        List<ContentItemVersionResponseDTO> contentItems = contentItemService.findByIds(extractedVersionIds);

        return new SearchResultsWithContent<>(response, contentItems);
    }

    private SearchRequest.Builder builderFromJSON(String searchRequestJSON) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.withJson(new ByteArrayInputStream(searchRequestJSON.getBytes()));
        return builder;
    }

    // A hit with no _source (e.g. a query with _source disabled) or no versionId field can't be
    // hydrated from Postgres; skip it rather than fail the whole search with a NullPointerException.
    private static UUID extractVersionId(Hit<EntityAsMap> hit) {
        var source = hit.source();
        var versionId = source == null ? null : (String) source.get("versionId");
        return StringUtils.isBlank(versionId) ? null : UUID.fromString(versionId);
    }
}
