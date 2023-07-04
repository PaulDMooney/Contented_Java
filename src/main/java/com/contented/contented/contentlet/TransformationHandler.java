package com.contented.contented.contentlet;

import com.contented.contented.contentlet.transformation.ContentletEntityTransformer;
import com.contented.contented.contentlet.transformation.Transformer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Log4j2
public class TransformationHandler {

    private final List<ContentletEntityTransformer> transformers;

    @Autowired
    public TransformationHandler(List<ContentletEntityTransformer> transformers) {
        this.transformers = transformers;
    }

    public ContentletEntity applyTransformation(ContentletEntity entity) {
        return transformers.stream()
            .filter(transformer -> transformer.test(entity))
            .findFirst()
            .map(transformer -> transformer.transform(entity))
            .or(() -> {
                log.info("No transformer found");
                return Optional.of(entity);
            }).get();
    }
}
