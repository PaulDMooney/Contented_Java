package com.contented.contented.contentlet.elasticsearch.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static com.contented.contented.contentlet.elasticsearch.transformation.StandardContentletTransformations.applyStandardTransformations;
import static com.contented.contented.contentlet.transformation.StandardDMSContentTransformer.BLOG_VALUE;
import static com.contented.contented.contentlet.transformation.StandardDMSContentTransformer.CONTENT_TYPE_FIELD;

@Component
public class BlogTransformer implements ESRecordTransformer {
    @Override
    public Collection<EntityAsMap> transform(ContentletEntity toTransform) {

        EntityAsMap entityAsMap = new EntityAsMap();
        applyStandardTransformations(toTransform, entityAsMap);
        entityAsMap.put("blog.title", toTransform.get("title"));

        return List.of(entityAsMap);
    }

    @Override
    public boolean test(ContentletEntity contentletEntity) {
        return BLOG_VALUE.equals(contentletEntity.getSchemalessData().get(CONTENT_TYPE_FIELD));
    }
}
