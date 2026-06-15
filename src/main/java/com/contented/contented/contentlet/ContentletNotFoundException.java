package com.contented.contented.contentlet;

import java.util.UUID;

public class ContentletNotFoundException extends RuntimeException {

    private final UUID id;

    public ContentletNotFoundException(UUID id) {
        super("Contentlet `" + id + "` was not found");
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
