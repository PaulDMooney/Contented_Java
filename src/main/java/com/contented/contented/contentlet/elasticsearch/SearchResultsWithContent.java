package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import com.contented.contented.contentlet.ContentletEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

public record SearchResultsWithContent<T>(
    @JsonSerialize(using = SearchResponseSerializer.class) SearchResponse<T> esResponse,
    List<ContentletEntity> contentlets) {
}
