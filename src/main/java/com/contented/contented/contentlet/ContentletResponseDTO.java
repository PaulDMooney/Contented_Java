package com.contented.contented.contentlet;

public class ContentletResponseDTO extends AbstractContentletDTO {

    public static ContentletResponseDTO from(ContentletEntity entity) {
        var dto = new ContentletResponseDTO();
        dto.setId(entity.getId());
        dto.setContentType(entity.getContentType());
        entity.getSchemalessData().forEach(dto::add);
        return dto;
    }
}
