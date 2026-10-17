package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.common.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StandardDMSContentTransformer")
class StandardDMSContentTransformerTest {

    Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    StandardDMSContentTransformer transformer = new StandardDMSContentTransformer(clock);

    @Nested
    @DisplayName("`transform()`")
    class TransformTests {

        @Nested
        @DisplayName("When called with a `ContentItemEntity` that has no `id`")
        class WhenCalledWithNoId {

            ContentItemEntity contentItemEntity = new ContentItemEntity(null, "Blog",
                Map.ofEntries(entry("language", "EN"))
            );

            @Test
            @DisplayName("It should normalize the `language` to lowercase")
            void it_should_normalize_language_to_lowercase() {
                // When
                var result = transformer.transform(contentItemEntity);

                // Then
                assertThat((String) result.get("language")).isEqualTo("en");
            }

            @Test
            @DisplayName("It should leave the `id` unset for the service to assign")
            void it_should_leave_the_id_unset() {
                // When
                var result = transformer.transform(contentItemEntity);

                // Then
                assertThat(result.getVersionId()).isNull();
            }

            @Test
            @DisplayName("It should populate the `modDate` with the current time")
            void it_should_populate_modDate_with_current_time() {
                // When
                var result = transformer.transform(contentItemEntity);

                // Then
                assertThat((Instant) result.get("modDate")).isEqualTo(clock.instant());
            }
        }

        @Nested
        @DisplayName("When called with a `ContentItemEntity` that has an `id`")
        class WhenCalledWithId {

            ContentItemEntity toSave = new ContentItemEntity(UuidV7.generate(), "Blog", Map.ofEntries(
                entry("language", "en")
            ));

            @Test
            @DisplayName("It should preserve the `id`")
            void it_should_preserve_the_id() {
                // When
                var result = transformer.transform(toSave);

                // Then
                assertThat(result.getVersionId()).isEqualTo(toSave.getVersionId());
            }
        }
    }

    @Nested
    @DisplayName("`test()`")
    class TestPredicate {

        @Test
        @DisplayName("It should match a `contentItem` whose `contentType` is supported")
        void it_should_match_supported_content_type() {
            var contentItemEntity = new ContentItemEntity(null, "Blog", Map.of());

            assertThat(transformer.test(contentItemEntity)).isTrue();
        }

        @Test
        @DisplayName("It should not match a `contentItem` whose `contentType` is unsupported")
        void it_should_not_match_unsupported_content_type() {
            var contentItemEntity = new ContentItemEntity(null, "SomethingElse", Map.of());

            assertThat(transformer.test(contentItemEntity)).isFalse();
        }
    }
}
