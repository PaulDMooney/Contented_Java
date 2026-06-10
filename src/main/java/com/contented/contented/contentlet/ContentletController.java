package com.contented.contented.contentlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return contentletRepository.findAll().collectList().block();
    }

    @GetMapping("/{id}")
    ResponseEntity<ContentletEntity> findById(@PathVariable String id) {
        return contentletService.findById(id)
                .map(contentletEntity -> ResponseEntity.ok(contentletEntity))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    ResponseEntity<ContentletEntity> putContentlet(@RequestBody ContentletDTO contentletDTO) {
        ContentletEntity toSave = new ContentletEntity(contentletDTO.getId(), contentletDTO.get());

        var resultPair = contentletService.save(toSave);
        var statusCode = resultPair.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(statusCode)
                .body(resultPair.contentletEntity());
    }

    @DeleteMapping("/{id}")
    void deleteContentlet(@PathVariable String id) {
        contentletService.deleteById(id);
    }
}
