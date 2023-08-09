package com.contented.contented.contentlet.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StandardDMSContentTransformer implements ContentletEntityTransformer {

    // TODO: Make any of these standard fields of the ContentletEntity class? TBD.
    // Legacy support field
    public static final String ST_NAME_FIELD = "stName";
    public static final String CONTENT_TYPE_FIELD = "contentType";

    // Language agnostic identifier. Legacy Support field.
    public static final String IDENTIFIER_FIELD = "identifier";

    public static final String LANGUAGE_FIELD = "language";

    // Language specific identifier. Legacy support field
    public static final String INODE_FIELD = "inode";

    public static final String MODE_DATE_FIELD = "modDate";

    // Legacy support field
    public static final String LIVE_FIELD = "live";

    public static final String PARENT_DMS_ID_FIELD = "parentDmsId";

    public static final String DMS_ID_FIELD = "dmsId";

    // Legacy support field.
//    public static final String LANGUAGE_ID_FIELD = "languageId";

    public static final String BLOG_VALUE = "Blog";

    public static final List<String> SUPPORTED_TYPES = List.of(BLOG_VALUE);

    private final Clock clock;

    public StandardDMSContentTransformer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ContentletEntity transform(ContentletEntity toTransform) {

        Map<String, Object> mutableSchemalessData = new HashMap<>(toTransform.getSchemalessData());
        setContentTypeAndSTName(mutableSchemalessData);
        normalizeLanguage(mutableSchemalessData);
        setINode(mutableSchemalessData);
        setIdentifier(mutableSchemalessData);
        setModDate(mutableSchemalessData);
        var id = deriveId(toTransform, mutableSchemalessData);
        return new ContentletEntity(id, Collections.unmodifiableMap(mutableSchemalessData));
    }

    private String deriveId(ContentletEntity toTransform, Map<String, Object> mutableSchemalessData) {
        if (toTransform.getId() == null) {
            return (String) mutableSchemalessData.get(INODE_FIELD);
        }
        return toTransform.getId();
    }

    private void setModDate(Map<String, Object> mutableSchemalessData) {
        mutableSchemalessData.put(MODE_DATE_FIELD, clock.instant());
    }

    private void setINode(Map<String, Object> mutableSchemalessData) {
        mutableSchemalessData.put(INODE_FIELD,
                mutableSchemalessData.get(DMS_ID_FIELD));
    }

    private void normalizeLanguage(Map<String, Object> mutableSchemalessData) {
        mutableSchemalessData.compute(LANGUAGE_FIELD, (key, oldValue) -> {
            var oldValueStr = (String) oldValue;
            if (StringUtils.isBlank(oldValueStr)) {
                return null;
            }
            return oldValueStr.toLowerCase();
        });

    }

    private void setIdentifier(Map<String, Object> mutableSchemalessData) {
        mutableSchemalessData.put(IDENTIFIER_FIELD,
                mutableSchemalessData.get(PARENT_DMS_ID_FIELD));
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
        return SUPPORTED_TYPES.stream()
                .anyMatch(type ->
                        type.equalsIgnoreCase(stName) || BLOG_VALUE.equalsIgnoreCase(contentType)
                );
    }
}
