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

    public static final String LANGUAGE_FIELD = "language";

    public static final String MODE_DATE_FIELD = "modDate";

    public static final String BLOG_VALUE = "Blog";

    public static final List<String> SUPPORTED_TYPES = List.of(BLOG_VALUE);

    private final Clock clock;

    public StandardDMSContentTransformer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ContentletEntity transform(ContentletEntity toTransform) {

        Map<String, Object> mutableSchemalessData = new HashMap<>(toTransform.getSchemalessData());
        normalizeLanguage(mutableSchemalessData);
        setModDate(mutableSchemalessData);
        return new ContentletEntity(toTransform.getId(), toTransform.getContentType(),
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
    public boolean test(ContentletEntity contentletEntity) {
        String contentType = contentletEntity.getContentType();
        return SUPPORTED_TYPES.stream()
                .anyMatch(type -> type.equalsIgnoreCase(contentType));
    }
}
