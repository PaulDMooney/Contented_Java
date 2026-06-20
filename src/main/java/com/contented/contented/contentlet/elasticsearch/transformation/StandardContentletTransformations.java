package com.contented.contented.contentlet.elasticsearch.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

public class StandardContentletTransformations {

    public static EntityAsMap applyStandardTransformations(ContentletEntity toTransform, EntityAsMap toApplyTo) {
        toApplyTo.put(ContentletEntity.CONTENT_TYPE_FIELD, StringUtils.lowerCase(toTransform.getContentType()));
        toApplyTo.put("language", toTransform.get("language"));
        toApplyTo.put("identifier", toTransform.getId() + "_" + toTransform.get("language"));
        toApplyTo.put("id", toTransform.getId().toString());
        return toApplyTo;
    }
}
