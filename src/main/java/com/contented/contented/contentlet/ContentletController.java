package com.contented.contented.contentlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ContentletController.CONTENTLETS_PATH)
public class ContentletController {

    public static final String CONTENTLETS_PATH = "contentlets";

    final ContentletRepository contentletRepository;

    final ContentletService contentletService;

    @Autowired
    public ContentletController(ContentletRepository contentletRepository, ContentletService contentletService) {
        this.contentletRepository = contentletRepository;
        this.contentletService = contentletService;
    }

    @GetMapping("/all")
    List<ContentletEntity> getAll() {
        // TODO: Replace this with a paginated version in the future
        return contentletRepository.findAll();
    }

    @GetMapping("/{id}")
    ResponseEntity<ContentletEntity> findById(@PathVariable UUID id) {
        return contentletService.findById(id)
                .map(contentletEntity -> ResponseEntity.ok(contentletEntity))
                .orElseThrow(() -> new ContentletNotFoundException(id));
    }

    @PostMapping
    ResponseEntity<ContentletEntity> createContentlet(@RequestBody ContentletDTO contentletDTO,
                                                      UriComponentsBuilder uriBuilder) {
        // Ids are server-assigned; a client must not supply one when creating.
        if (contentletDTO.getId() != null) {
            throw new InvalidContentletException(
                    "A contentlet id must not be supplied when creating; ids are server-assigned.");
        }
        ContentletEntity toSave = new ContentletEntity(null, contentletDTO.get());

        var created = contentletService.create(toSave);
        var location = uriBuilder.path("/{base}/{id}")
                .buildAndExpand(CONTENTLETS_PATH, created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<ContentletEntity> updateContentlet(@PathVariable UUID id,
                                                      @RequestBody ContentletDTO contentletDTO) {
        // The URL is the source of truth for identity; a body id must agree with it.
        if (contentletDTO.getId() != null && !contentletDTO.getId().equals(id)) {
            throw new InvalidContentletException(
                    "The body id `" + contentletDTO.getId() + "` does not match the URL id `" + id + "`.");
        }
        ContentletEntity toSave = new ContentletEntity(id, contentletDTO.get());

        return contentletService.update(id, toSave)
                .map(updated -> ResponseEntity.ok(updated))
                .orElseThrow(() -> new ContentletNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    void deleteContentlet(@PathVariable UUID id) {
        contentletService.deleteById(id);
    }
}
