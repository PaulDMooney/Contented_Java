package com.contented.contented.contentitem.model;

import com.contented.contented.common.UuidV7;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContentItemMapper")
class ContentItemMapperTest {

    ContentItemMapper mapper = new ContentItemMapper();

    @Nested
    @DisplayName("`toResponse()`")
    class ToResponse {

        UUID versionId = UuidV7.generate();
        UUID identifier = UuidV7.generate();
        Instant created = Instant.now();
        ContentItemEntity entity = entity();
        ContentItemResponseDTO result;

        ContentItemEntity entity() {
            return ContentItemEntity.newVersion(versionId, identifier, "Blog", ContentItemState.LIVE, created,
                Map.of("title", "Hello"));
        }

        @BeforeAll
        void when() {
            result = mapper.toResponse(entity);
        }

        @Test
        @DisplayName("It should copy the version id, identifier, state and creation date")
        void it_should_copy_the_version_fields() {
            assertThat(result.getVersionId()).isEqualTo(versionId);
            assertThat(result.getIdentifier()).isEqualTo(identifier);
            assertThat(result.getState()).isEqualTo(ContentItemState.LIVE);
            assertThat(result.getVersionCreatedDatetime()).isEqualTo(created);
        }

        @Test
        @DisplayName("It should copy the `contentType`")
        void it_should_copy_the_contentType() {
            assertThat(result.getContentType()).isEqualTo("Blog");
        }

        @Test
        @DisplayName("It should copy each schemaless field into `data`")
        void it_should_copy_each_schemaless_field() {
            assertThat(result.getData()).containsEntry("title", "Hello");
        }

        @Test
        @DisplayName("It should not share the entity's backing map")
        void it_should_not_share_the_entitys_backing_map() {
            result.getData().put("addedToResponse", "value");

            assertThat((Object) entity.get("addedToResponse")).isNull();
        }
    }

    @Nested
    @DisplayName("`toSummary()`")
    class ToSummary {

        UUID versionId = UuidV7.generate();
        Instant created = Instant.now();
        ContentItemVersionSummaryDTO result;

        @BeforeAll
        void when() {
            var entity = ContentItemEntity.newVersion(versionId, UuidV7.generate(), "Blog",
                ContentItemState.ARCHIVED, created, Map.of("title", "Hello"));
            result = mapper.toSummary(entity);
        }

        @Test
        @DisplayName("It should carry the version id, state and creation date")
        void it_should_carry_the_summary_fields() {
            assertThat(result.versionId()).isEqualTo(versionId);
            assertThat(result.state()).isEqualTo(ContentItemState.ARCHIVED);
            assertThat(result.versionCreatedDatetime()).isEqualTo(created);
        }
    }

    @Nested
    @DisplayName("`toEntity()`")
    class ToEntity {

        ContentItemEntity result;

        @BeforeAll
        void when() {
            ContentItemDTO dto = new ContentItemDTO();
            dto.setContentType("Blog");
            dto.getData().put("title", "Hello");

            result = mapper.toEntity(dto);
        }

        @Test
        @DisplayName("It should copy the `contentType`")
        void it_should_copy_the_contentType() {
            assertThat(result.getContentType()).isEqualTo("Blog");
        }

        @Test
        @DisplayName("It should copy each `data` field into the entity")
        void it_should_copy_each_schemaless_field() {
            assertThat((String) result.get("title")).isEqualTo("Hello");
        }
    }
}
