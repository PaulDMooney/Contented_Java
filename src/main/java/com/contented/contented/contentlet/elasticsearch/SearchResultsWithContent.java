package com.contented.contented.contentlet.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.contentlet.ContentletResponseDTO;

import java.util.List;

// esResponse is serialized by the SearchResponseSerializer @JacksonComponent, registered with
// the application's central JsonMapper. A field-level @JsonSerialize(using = ...) would override
// that registration with a reflectively-created instance, bypassing the injected JsonpMapper.
public record SearchResultsWithContent<T>(
    SearchResponse<T> esResponse,
    List<ContentletResponseDTO> contentlets) {
}
