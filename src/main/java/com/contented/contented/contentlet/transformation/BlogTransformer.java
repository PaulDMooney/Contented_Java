package com.contented.contented.contentlet.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class BlogTransformer implements ContentletEntityTransformer {

    public static final String ST_NAME_FIELD = "stName";
    public static final String CONTENT_TYPE_FIELD = "contentType";

    public static final String IDENTIFIER_FIELD = "identifier";

    public static final String LANGUAGE_FIELD = "language";

    public static final String BLOG_VALUE = "Blog";

    @Override
    public ContentletEntity transform(ContentletEntity toTransform) {

        Map<String, Object> mutableSchemalessData = new HashMap<>(toTransform.getSchemalessData());
        setContentTypeAndSTName(mutableSchemalessData);
        normalizeLanguage(mutableSchemalessData);
        setIdentifier(toTransform,mutableSchemalessData);
        return new ContentletEntity(toTransform.getId(), Collections.unmodifiableMap(mutableSchemalessData));
    }

    private void normalizeLanguage(Map<String, Object> mutableSchemalessData) {
        mutableSchemalessData.compute(LANGUAGE_FIELD, (key, oldValue) ->
            ((String) oldValue).toLowerCase());
    }

    private void setIdentifier(ContentletEntity original, Map<String, Object> mutableSchemalessData) {
        String identifierFormat = "%s_%s";
        mutableSchemalessData.compute(IDENTIFIER_FIELD, (key, oldValue) ->
                String.format(identifierFormat, original.getId(),
                    mutableSchemalessData.get(LANGUAGE_FIELD))
        );
    }

    private void setContentTypeAndSTName(Map<String, Object> mutableSchemalessData) {

        // One of these two fields must be populated.
        mutableSchemalessData.computeIfAbsent(CONTENT_TYPE_FIELD,
            key -> mutableSchemalessData.get(ST_NAME_FIELD));
        mutableSchemalessData.computeIfAbsent(ST_NAME_FIELD,
            key -> mutableSchemalessData.get(CONTENT_TYPE_FIELD));

        // TODO: Throw an exception if the two fields don't match.
    }

    @Override
    public boolean test(ContentletEntity contentletEntity) {
        String stName = (String) contentletEntity.getSchemalessData().get(ST_NAME_FIELD);
        String contentType = (String) contentletEntity.getSchemalessData().get(CONTENT_TYPE_FIELD);
        return BLOG_VALUE.equalsIgnoreCase(stName) || BLOG_VALUE.equalsIgnoreCase(contentType);
    }
}
