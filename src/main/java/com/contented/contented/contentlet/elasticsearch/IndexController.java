package com.contented.contented.contentlet.elasticsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(IndexController.INDEX_PATH)
public class IndexController {
    public static final String INDEX_PATH = "index";

    private final ElasticSearchIndexCreator elasticSearchIndexCreator;

    @Autowired
    public IndexController(ElasticSearchIndexCreator elasticSearchIndexCreator) {
        this.elasticSearchIndexCreator = elasticSearchIndexCreator;
    }

    // TODO: Temporary, A better design would allow for creating any index name, and then assign an alias to it.
    @PutMapping("/create")
    public ResponseEntity createIndex() {
        boolean result = elasticSearchIndexCreator.createIndex();
        return result ? ResponseEntity.ok().build() : ResponseEntity.internalServerError().build();
    }
}
