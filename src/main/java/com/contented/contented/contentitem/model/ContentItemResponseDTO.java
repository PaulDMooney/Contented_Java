package com.contented.contented.contentitem.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ContentItemResponseDTO extends AbstractContentItemDTO {

    // This exact version's id.
    private UUID versionId;

    // The version-agnostic id of the logical content this version belongs to.
    private UUID identifier;

    private ContentItemState state;

    private Instant versionCreatedDatetime;
}
