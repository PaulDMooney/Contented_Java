package com.contented.contented.contentitem.model;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class ContentItemMapper {

    public ContentItemEntity toEntity(ContentItemDTO dto) {
        return new ContentItemEntity(dto.getId(), dto.getContentType(), dto.get());
    }

    public ContentItemResponseDTO toResponse(ContentItemEntity entity) {
        var dto = new ContentItemResponseDTO();
        dto.setId(entity.getId());
        dto.setContentType(entity.getContentType());
        dto.setSchemalessData(new LinkedHashMap<>(entity.getSchemalessData()));
        return dto;
    }
}
