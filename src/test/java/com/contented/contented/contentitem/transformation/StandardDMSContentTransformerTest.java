package com.contented.contented.contentitem.transformation;

import com.contented.contented.contentitem.ContentItemEntity;
import com.contented.contented.contentitem.UuidV7;
import com.contented.contented.contentitem.testutils.NestedPerClass;
import org.junit.jupiter.api.DisplayName;
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

    @NestedPerClass
    @DisplayName("transform")
    class TransformTests {

        @NestedPerClass
        @DisplayName("Given a contentItemEntity with no id")
        class GivenContentItemWithNoId {

            ContentItemEntity contentItemEntity = new ContentItemEntity(null, "Blog",
                Map.ofEntries(entry("language", "EN"))
            );

            @Test
            @DisplayName("it should normalize the `language` to lowercase")
            void it_should_normalize_language_to_lowercase() {
                // When
                var result = transformer.transform(contentItemEntity);

                // Then
                assertThat((String) result.get("language")).isEqualTo("en");
            }

            @Test
            @DisplayName("it should leave the id unset for the service to assign")
            void it_should_leave_the_id_unset() {
                // When
                var result = transformer.transform(contentItemEntity);

                // Then
                assertThat(result.getId()).isNull();
            }

            @Test
            @DisplayName("it should populate the modDate with the current time")
            void it_should_populate_modDate_with_current_time() {
                // When
                var result = transformer.transform(contentItemEntity);

                // Then
                assertThat((Instant) result.get("modDate")).isEqualTo(clock.instant());
            }
        }

        @NestedPerClass
        @DisplayName("Given a contentItemEntity with an id")
        class GivenContentItemWithId {

            ContentItemEntity toSave = new ContentItemEntity(UuidV7.generate(), "Blog", Map.ofEntries(
                entry("language", "en")
            ));

            @Test
            @DisplayName("it should preserve the id")
            void it_should_preserve_the_id() {
                // When
                var result = transformer.transform(toSave);

                // Then
                assertThat(result.getId()).isEqualTo(toSave.getId());
            }
        }
    }

    @NestedPerClass
    @DisplayName("test")
    class TestPredicate {

        @Test
        @DisplayName("it should match a contentItem whose contentType is supported")
        void it_should_match_supported_content_type() {
            var contentItemEntity = new ContentItemEntity(null, "Blog", Map.of());

            assertThat(transformer.test(contentItemEntity)).isTrue();
        }

        @Test
        @DisplayName("it should not match a contentItem whose contentType is unsupported")
        void it_should_not_match_unsupported_content_type() {
            var contentItemEntity = new ContentItemEntity(null, "SomethingElse", Map.of());

            assertThat(transformer.test(contentItemEntity)).isFalse();
        }
    }
}
