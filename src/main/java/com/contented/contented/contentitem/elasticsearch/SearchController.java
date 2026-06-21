package com.contented.contented.contentitem.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.contentitem.ContentItemResponseDTO;
import com.contented.contented.contentitem.ContentItemService;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
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

        List<UUID> extractedIds = response.hits().hits().stream()
            .map(hit -> (String) hit.source().get("id"))
            .map(UUID::fromString)
            .toList();

        List<ContentItemResponseDTO> contentItems = contentItemService.findByIds(extractedIds);

        return new SearchResultsWithContent<>(response, contentItems);
    }

    private SearchRequest.Builder builderFromJSON(String searchRequestJSON) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.withJson(new ByteArrayInputStream(searchRequestJSON.getBytes()));
        return builder;
    }
}
