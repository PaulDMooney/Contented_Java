package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Log4j2
public class TransformationHandler {

    private final List<ContentItemEntityTransformer> transformers;

    @Autowired
    public TransformationHandler(List<ContentItemEntityTransformer> transformers) {
        this.transformers = transformers;
    }

    public ContentItemEntity applyTransformation(ContentItemEntity entity) {
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
