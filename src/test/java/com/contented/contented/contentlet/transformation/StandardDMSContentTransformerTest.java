package com.contented.contented.contentlet.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.testutils.NestedPerClass;
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

    @NestedPerClass
    @DisplayName("transform")
    class TransformTests {

        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        StandardDMSContentTransformer transformer = new StandardDMSContentTransformer(clock);

        @NestedPerClass
        @DisplayName("Given a contentletEntity with no id")
        class GivenContentletWithNoId {

            ContentletEntity contentletEntity = new ContentletEntity(null,
                Map.ofEntries(entry("language", "en"),
                    entry("dmsId", "dmsid1234"),
                    entry("parentDmsId", "parentdmsid1234"),
                    entry("stName", "Blog")
                )
            );

            @Test
            @DisplayName("it should populate the `inode` from the `dmsId`")
            void it_should_populate_inode_from_dmsId() {
                // When
                var result = transformer.transform(contentletEntity);

                // Then
                assertThat((String) result.get("inode")).isEqualTo(contentletEntity.get("dmsId"));
            }

            @Test
            @DisplayName("it should populate the `identifier` from the `parentDmsId`")
            void it_should_populate_identifier_from_parentDmsId() {
                // When
                var result = transformer.transform(contentletEntity);

                // Then
                assertThat((String) result.get("identifier")).isEqualTo(contentletEntity.get("parentDmsId"));
            }

            @Test
            @DisplayName("it should populate the `contentType` from the `stName`")
            void it_should_populate_contentType_from_stName() {
                // When
                var result = transformer.transform(contentletEntity);

                // Then
                assertThat((String) result.get("contentType")).isEqualTo(contentletEntity.get("stName"));
            }

            @Test
            @DisplayName("it should populate the `id` field from the `dmsId`")
            void it_should_populate_id_field_from_dmsId() {
                // When
                var result = transformer.transform(contentletEntity);

                // Then
                assertThat(result.getId()).isEqualTo(contentletEntity.get("dmsId"));
            }

            @Test
            @DisplayName("it should populate the modDate with the current time")
            void it_should_populate_modDate_with_current_time() {
                // When
                var result = transformer.transform(contentletEntity);

                // Then
                assertThat((Instant) result.get("modDate")).isEqualTo(clock.instant());
            }
        }

        @NestedPerClass
        @DisplayName("Given a contentletEntity with an id")
        class GivenContentletWithId {

            ContentletEntity toSave = new ContentletEntity("Contentlet1", Map.ofEntries(
                entry("language", "en"),
                entry("dmsId", "dmsid1234"),
                entry("inode", "inode1234"),
                entry("parentDmsId", "parentdmsid1234"),
                entry("stName", "Blog")
            ));

            @Test
            @DisplayName("it should not override the id with the dmsId or inode")
            void it_should_not_override_the_id_with_the_dmsId_or_inode() {
                // When
                var result = transformer.transform(toSave);

                // Then
                assertThat(result.getId()).isEqualTo(toSave.getId());
            }
        }
    }
}