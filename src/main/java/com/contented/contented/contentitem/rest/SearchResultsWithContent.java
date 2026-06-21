package com.contented.contented.contentitem.rest;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;

import java.util.List;

// esResponse is serialized by the SearchResponseSerializer @JacksonComponent, registered with
// the application's central JsonMapper. The serializer has no no-arg constructor, so it can only
// be built with the injected JsonpMapper — a field-level @JsonSerialize(using = ...) won't compile
// a working path here, which keeps the injected mapper as the only one ever used.
public record SearchResultsWithContent<T>(
    SearchResponse<T> esResponse,
    List<ContentItemResponseDTO> contentItems) {
}
