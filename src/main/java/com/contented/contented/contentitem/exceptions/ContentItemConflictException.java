package com.contented.contented.contentitem.exceptions;

/**
 * The content exists but is in a state where the requested operation does not apply — distinct
 * from {@link ContentItemNotFoundException} (the content doesn't exist at all) and
 * {@link InvalidContentItemException} (the request itself is invalid).
 */
public class ContentItemConflictException extends RuntimeException {

    public ContentItemConflictException(String message) {
        super(message);
    }
}
