package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.contentlet.ContentletService;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping(SearchController.SEARCH_PATH)
public class SearchController {
    public static final String SEARCH_PATH = "search";

    private final ReactiveElasticsearchClient reactiveElasticsearchClient;

    private final IndexCoordinates indexCoordinates;

    private final ContentletService contentletService;


    public SearchController(ReactiveElasticsearchClient reactiveElasticsearchClient, IndexCoordinates indexCoordinates, ContentletService contentletService) {
        this.reactiveElasticsearchClient = reactiveElasticsearchClient;
        this.indexCoordinates = indexCoordinates;
        this.contentletService = contentletService;
    }

    @PostMapping("/withcontent")
    public Mono<SearchResultsWithContent<EntityAsMap>> searchWithContent(@RequestBody String searchRequestJSON) {

        SearchRequest request = builderFromJSON(searchRequestJSON)
            .index(indexCoordinates.getIndexName()).build();

        return reactiveElasticsearchClient.search(request, EntityAsMap.class)
            .flatMap(response -> {
                List<String> extractedIds = response.hits().hits().stream()
                    .map(hit -> (String) hit.source().get("id"))
                    .toList();

                return contentletService.findByIds(extractedIds)
                    .collectList()
                    .map(contentlets -> new SearchResultsWithContent<>((SearchResponse<EntityAsMap>) response, contentlets));

            });
    }

    private SearchRequest.Builder builderFromJSON(String searchRequestJSON) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.withJson(new ByteArrayInputStream(searchRequestJSON.getBytes()));
        return builder;
    }
}
