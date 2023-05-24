package com.contented.contented.contentlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    Flux<ContentletEntity> getAll() {
        // TODO: Replace this with a paginated version in the future
        return contentletRepository.findAll();
    }

    @GetMapping("/{id}")
    Mono<ResponseEntity<ContentletEntity>> findById(@PathVariable String id) {
        return contentletService.findById(id)
                .map(contentletEntity -> ResponseEntity.ok(contentletEntity))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping
    Mono<ResponseEntity<ContentletEntity>> putContentlet(@RequestBody ContentletDTO contentletDTO) {
        ContentletEntity toSave = new ContentletEntity(contentletDTO.getId(), contentletDTO.get());

        return contentletService.save(toSave)
                .map(resultPair -> {
                    var statusCode = resultPair.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
                    return ResponseEntity.status(statusCode)
                            .body(resultPair.contentletEntity());
                });
    }

    @DeleteMapping("/{id}")
    Mono<Void> deleteContentlet(@PathVariable String id) {
        return contentletService.deleteById(id);
    }
}
