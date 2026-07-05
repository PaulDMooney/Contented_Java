package com.contented.contented.contentitem.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
@Jacksonized
public class ContentItemResponseDTO extends AbstractContentItemDTO {

    // This exact version's id.
    private final UUID versionId;

    // The version-agnostic id of the logical content this version belongs to.
    private final UUID identifier;

    private final ContentItemState state;

    private final Instant versionCreatedDatetime;
}
