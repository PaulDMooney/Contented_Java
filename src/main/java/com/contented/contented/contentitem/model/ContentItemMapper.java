package com.contented.contented.contentitem.model;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class ContentItemMapper {

    public ContentItemEntity toEntity(ContentItemDTO dto) {
        // Identity is assigned by the service, not the client; the entity is built id-less here.
        return new ContentItemEntity(null, dto.getContentType(), dto.getData());
    }

    public ContentItemResponseDTO toResponse(ContentItemEntity entity) {
        return ContentItemResponseDTO.builder()
            .versionId(entity.getVersionId())
            .identifier(entity.getIdentifier())
            .contentType(entity.getContentType())
            .state(entity.getState())
            .versionCreatedDatetime(entity.getVersionCreatedDatetime())
            .data(new LinkedHashMap<>(entity.getSchemalessData()))
            .build();
    }

    public ContentItemVersionSummaryDTO toSummary(ContentItemEntity entity) {
        return new ContentItemVersionSummaryDTO(entity.getVersionId(), entity.getState(), entity.getVersionCreatedDatetime());
    }
}
