package com.contented.contented.contentlet;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class ContentletMapper {

    public ContentletEntity toEntity(ContentletDTO dto) {
        return new ContentletEntity(dto.getId(), dto.getContentType(), dto.get());
    }

    public ContentletResponseDTO toResponse(ContentletEntity entity) {
        var dto = new ContentletResponseDTO();
        dto.setId(entity.getId());
        dto.setContentType(entity.getContentType());
        dto.setSchemalessData(new LinkedHashMap<>(entity.getSchemalessData()));
        return dto;
    }
}
