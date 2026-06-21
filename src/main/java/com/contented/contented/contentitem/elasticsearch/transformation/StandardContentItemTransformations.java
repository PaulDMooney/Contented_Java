package com.contented.contented.contentitem.elasticsearch.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

public class StandardContentItemTransformations {

    public static EntityAsMap applyStandardTransformations(ContentItemEntity toTransform, EntityAsMap toApplyTo) {
        toApplyTo.put(ContentItemEntity.CONTENT_TYPE_FIELD, StringUtils.lowerCase(toTransform.getContentType()));
        toApplyTo.put("language", toTransform.get("language"));
        toApplyTo.put("identifier", toTransform.getId() + "_" + toTransform.get("language"));
        toApplyTo.put("id", toTransform.getId().toString());
        return toApplyTo;
    }
}
