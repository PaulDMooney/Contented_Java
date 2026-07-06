package com.contented.contented.contentitem.elasticsearch.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

public class StandardContentItemTransformations {

    public static EntityAsMap applyStandardTransformations(ContentItemEntity toTransform, EntityAsMap toApplyTo) {
        toApplyTo.put(ContentItemEntity.CONTENT_TYPE_FIELD, StringUtils.lowerCase(toTransform.getContentType()));
        toApplyTo.put("language", toTransform.get("language"));
        // The ES document id is the version-agnostic identifier, so each publish overwrites the
        // single live document for that content.
        toApplyTo.put("id", toTransform.getIdentifier().toString());
        // The exact source row, used to hydrate the precise live version from the database.
        toApplyTo.put("versionId", toTransform.getVersionId().toString());
        return toApplyTo;
    }
}
