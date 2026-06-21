package com.contented.contented.contentitem.model;

import java.util.UUID;

public class ContentItemDTO extends AbstractContentItemDTO {

    public ContentItemDTO() {
    }

    public ContentItemDTO(UUID id) {
        setId(id);
    }
}
