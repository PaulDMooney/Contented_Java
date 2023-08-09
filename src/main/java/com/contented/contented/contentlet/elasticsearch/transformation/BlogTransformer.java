package com.contented.contented.contentlet.elasticsearch.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;
import org.springframework.stereotype.Component;

import static com.contented.contented.contentlet.transformation.StandardDMSContentTransformer.BLOG_VALUE;
import static com.contented.contented.contentlet.transformation.StandardDMSContentTransformer.CONTENT_TYPE_FIELD;

@Component
public class BlogTransformer implements ESRecordTransformer {
    @Override
    public EntityAsMap transform(ContentletEntity toTransform) {

        EntityAsMap entityAsMap = new EntityAsMap();
        entityAsMap.put("contentType", StringUtils.lowerCase(toTransform.get("contentType")));
        entityAsMap.put("language", toTransform.get("language"));
        entityAsMap.put("identifier", toTransform.getId());
        entityAsMap.put("id", toTransform.getId());
        entityAsMap.put("blog.title", toTransform.get("title"));

        return entityAsMap;
    }

    @Override
    public boolean test(ContentletEntity contentletEntity) {
        return BLOG_VALUE.equals(contentletEntity.getSchemalessData().get(CONTENT_TYPE_FIELD));
    }
}
