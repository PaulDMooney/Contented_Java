package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StandardDMSContentTransformer implements ContentItemEntityTransformer {

    public static final String LANGUAGE_FIELD = "language";

    public static final String MODE_DATE_FIELD = "modDate";

    public static final String BLOG_VALUE = "Blog";

    public static final List<String> SUPPORTED_TYPES = List.of(BLOG_VALUE);

    private final Clock clock;

    public StandardDMSContentTransformer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ContentItemEntity transform(ContentItemEntity toTransform) {

        Map<String, Object> mutableSchemalessData = new HashMap<>(toTransform.getSchemalessData());
        normalizeLanguage(mutableSchemalessData);
        setModDate(mutableSchemalessData);
        return new ContentItemEntity(toTransform.getId(), toTransform.getContentType(),
                Collections.unmodifiableMap(mutableSchemalessData));
    }

    private void setModDate(Map<String, Object> mutableSchemalessData) {
        mutableSchemalessData.put(MODE_DATE_FIELD, clock.instant());
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

    @Override
    public boolean test(ContentItemEntity contentItemEntity) {
        String contentType = contentItemEntity.getContentType();
        return SUPPORTED_TYPES.stream()
                .anyMatch(type -> type.equalsIgnoreCase(contentType));
    }
}
