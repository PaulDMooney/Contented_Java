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
        var dto = new ContentItemResponseDTO();
        dto.setVersionId(entity.getVersionId());
        dto.setIdentifier(entity.getIdentifier());
        dto.setContentType(entity.getContentType());
        dto.setState(entity.getState());
        dto.setVersionCreatedDatetime(entity.getVersionCreatedDatetime());
        dto.setData(new LinkedHashMap<>(entity.getSchemalessData()));
        return dto;
    }

    public ContentItemVersionSummaryDTO toSummary(ContentItemEntity entity) {
        return new ContentItemVersionSummaryDTO(entity.getVersionId(), entity.getState(), entity.getVersionCreatedDatetime());
    }
}
