package com.contented.contented.contentitem.exceptions;

import java.util.UUID;

public class ContentItemNotFoundException extends RuntimeException {

    private final UUID id;

    public ContentItemNotFoundException(UUID id) {
        super("ContentItem `" + id + "` was not found");
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
