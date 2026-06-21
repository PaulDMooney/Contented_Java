package com.contented.contented.contentitem;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public ContentItemController(ContentItemService contentItemService) {
        this.contentItemService = contentItemService;
    }

    @GetMapping("/all")
    List<ContentItemResponseDTO> getAll() {
        // TODO: Replace this with a paginated version in the future
        return contentItemService.findAll();
    }

    @GetMapping("/{id}")
    ResponseEntity<ContentItemResponseDTO> findById(@PathVariable UUID id) {
        return contentItemService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(id));
    }

    @PostMapping
    ResponseEntity<ContentItemResponseDTO> createContentItem(@RequestBody ContentItemDTO contentItemDTO,
                                                           UriComponentsBuilder uriBuilder) {
        // Ids are server-assigned; a client must not supply one when creating.
        if (contentItemDTO.getId() != null) {
            throw new InvalidContentItemException(
                    "A contentItem id must not be supplied when creating; ids are server-assigned.");
        }
        var created = contentItemService.create(contentItemDTO);
        var location = uriBuilder.path("/{base}/{id}")
                .buildAndExpand(CONTENTITEMS_PATH, created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<ContentItemResponseDTO> updateContentItem(@PathVariable UUID id,
                                                           @RequestBody ContentItemDTO contentItemDTO) {
        // The URL is the source of truth for identity; a body id must agree with it.
        if (contentItemDTO.getId() != null && !contentItemDTO.getId().equals(id)) {
            throw new InvalidContentItemException(
                    "The body id `" + contentItemDTO.getId() + "` does not match the URL id `" + id + "`.");
        }
        return contentItemService.update(id, contentItemDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ContentItemNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    void deleteContentItem(@PathVariable UUID id) {
        contentItemService.deleteById(id);
    }
}
