package com.contented.contented.contentitem.elasticsearch.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static com.contented.contented.contentitem.elasticsearch.transformation.StandardContentItemTransformations.applyStandardTransformations;
import static com.contented.contented.contentitem.transformation.StandardDMSContentTransformer.BLOG_VALUE;

@Component
public class BlogTransformer implements ESRecordTransformer {
    @Override
    public Collection<EntityAsMap> transform(ContentItemEntity toTransform) {

        EntityAsMap entityAsMap = new EntityAsMap();
        applyStandardTransformations(toTransform, entityAsMap);
        entityAsMap.put("blog.title", toTransform.get("title"));

        return List.of(entityAsMap);
    }

    @Override
    public boolean test(ContentItemEntity contentItemEntity) {
        return BLOG_VALUE.equalsIgnoreCase(contentItemEntity.getContentType());
    }
}
