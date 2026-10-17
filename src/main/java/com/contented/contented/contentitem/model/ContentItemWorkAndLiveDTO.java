package com.contented.contented.contentitem.model;

/**
 * The full editorial state of one logical content: its current {@code working} draft and current
 * {@code live} version. Either may be {@code null} (e.g. an unpublished draft has no live version;
 * freshly published content has no working version), so a UI can tell what exists in one read.
 */
public record ContentItemWorkAndLiveDTO(ContentItemResponseDTO working, ContentItemResponseDTO live) {
}
