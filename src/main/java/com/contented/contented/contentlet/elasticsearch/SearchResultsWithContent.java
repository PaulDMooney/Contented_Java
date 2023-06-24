package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import com.contented.contented.contentlet.ContentletEntity;

import java.util.List;

public record SearchResultsWithContent<T>(ResponseBody<T> esResponse, List<ContentletEntity> contentlets) {
}
