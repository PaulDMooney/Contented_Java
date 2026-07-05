package com.contented.contented.contentitem.rest;

import com.contented.contented.contentitem.ContentItemService;
import com.contented.contented.contentitem.exceptions.ContentItemNotFoundException;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.model.ContentItemStateDTO;
import com.contented.contented.contentitem.model.ContentItemVersionSummaryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ContentItemController.CONTENTITEMS_PATH)
public class ContentItemController {

    public static final String CONTENTITEMS_PATH = "contentitems";

    final ContentItemService contentItemService;

    public ContentItemController(ContentItemService contentItemService) {
        this.contentItemService = contentItemService;
    }

    @PostMapping
    ResponseEntity<ContentItemResponseDTO> createContentItem(@RequestBody ContentItemDTO contentItemDTO,
                                                           UriComponentsBuilder uriBuilder) {
        var created = contentItemService.create(contentItemDTO);
        var location = uriBuilder.path("/{base}/{identifier}")
                .buildAndExpand(CONTENTITEMS_PATH, created.getIdentifier())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{identifier}")
    ResponseEntity<ContentItemResponseDTO> editWorking(@PathVariable UUID identifier,
                                                       @RequestBody ContentItemDTO contentItemDTO) {
        return contentItemService.editWorking(identifier, contentItemDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(identifier));
    }

    @PostMapping("/{identifier}/publish")
    ResponseEntity<ContentItemResponseDTO> publish(@PathVariable UUID identifier) {
        return contentItemService.publish(identifier)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(identifier));
    }

    @GetMapping("/{identifier}")
    ResponseEntity<ContentItemStateDTO> getContentState(@PathVariable UUID identifier) {
        return contentItemService.getContentState(identifier)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(identifier));
    }

    @GetMapping("/{identifier}/versions")
    ResponseEntity<List<ContentItemVersionSummaryDTO>> getVersions(@PathVariable UUID identifier) {
        return contentItemService.getVersions(identifier)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(identifier));
    }

    @GetMapping("/{identifier}/versions/{versionId}")
    ResponseEntity<ContentItemResponseDTO> getVersion(@PathVariable UUID identifier,
                                                      @PathVariable UUID versionId) {
        return contentItemService.getVersion(identifier, versionId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(versionId));
    }

    @PostMapping("/{identifier}/versions/{versionId}/restore")
    ResponseEntity<ContentItemResponseDTO> restore(@PathVariable UUID identifier,
                                                   @PathVariable UUID versionId) {
        return contentItemService.restore(identifier, versionId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(versionId));
    }

    @DeleteMapping("/{identifier}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    void deleteContentItem(@PathVariable UUID identifier) {
        contentItemService.deleteByIdentifier(identifier);
    }
}
