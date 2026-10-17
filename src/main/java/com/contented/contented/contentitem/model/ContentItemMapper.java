package com.contented.contented.contentitem.model;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class ContentItemMapper {

    public ContentItemVersionEntity toEntity(ContentItemVersionDTO dto) {
        // Identity is assigned by the service, not the client; the entity is built id-less here.
        return new ContentItemVersionEntity(null, dto.getContentType(), dto.getData());
    }

    public ContentItemVersionResponseDTO toResponse(ContentItemVersionEntity entity) {
        return ContentItemVersionResponseDTO.builder()
            .versionId(entity.getVersionId())
            .identifier(entity.getIdentifier())
            .contentType(entity.getContentType())
            .state(entity.getState())
            .versionCreatedDatetime(entity.getVersionCreatedDatetime())
            .data(new LinkedHashMap<>(entity.getData()))
            .build();
    }

    public ContentItemVersionSummaryDTO toSummary(ContentItemVersionEntity entity) {
        return new ContentItemVersionSummaryDTO(entity.getVersionId(), entity.getState(), entity.getVersionCreatedDatetime());
    }
}
