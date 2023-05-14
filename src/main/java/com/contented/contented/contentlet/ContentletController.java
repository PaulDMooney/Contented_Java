package com.contented.contented.contentlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ContentletController.CONTENTLETS_PATH)
public class ContentletController {

    public static final String CONTENTLETS_PATH = "contentlets";

    final ContentletRepository contentletRepository;

    @Autowired
    public ContentletController(ContentletRepository contentletRepository) {
        this.contentletRepository = contentletRepository;
    }

    @RequestMapping("/all")
    Flux<ContentletEntity> getAll() {
        // TODO: Replace this with a paginated version in the future
        return contentletRepository.findAll();
    }

    @PutMapping
    Mono<ResponseEntity<ContentletEntity>> putContentlet(@RequestBody ContentletDTO contentletDTO) {
        ContentletEntity toSave = new ContentletEntity(contentletDTO.getId());
        return contentletRepository.save(toSave)
                .map(savedContentlet -> ResponseEntity.status(HttpStatusCode.valueOf(201))
                        .body(savedContentlet));
    }
}
