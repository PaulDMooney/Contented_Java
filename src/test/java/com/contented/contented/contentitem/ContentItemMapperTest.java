package com.contented.contented.contentitem;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.model.ContentItemDTO;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemMapper;
import com.contented.contented.contentitem.model.ContentItemResponseDTO;
import com.contented.contented.contentitem.testutils.NestedPerClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContentItemMapper")
class ContentItemMapperTest {

    ContentItemMapper mapper = new ContentItemMapper();

    @NestedPerClass
    @DisplayName("toResponse")
    class ToResponse {

        UUID id = UuidV7.generate();
        ContentItemEntity entity = new ContentItemEntity(id, "Blog", Map.of("title", "Hello"));
        ContentItemResponseDTO result;

        @BeforeAll
        void when() {
            result = mapper.toResponse(entity);
        }

        @Test
        @DisplayName("it should copy the id")
        void it_should_copy_the_id() {
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("it should copy the contentType")
        void it_should_copy_the_contentType() {
            assertThat(result.getContentType()).isEqualTo("Blog");
        }

        @Test
        @DisplayName("it should copy each schemaless field")
        void it_should_copy_each_schemaless_field() {
            assertThat(result.get()).containsEntry("title", "Hello");
        }

        @Test
        @DisplayName("it should not share the entity's backing map")
        void it_should_not_share_the_entitys_backing_map() {
            result.add("addedToResponse", "value");

            assertThat((Object) entity.get("addedToResponse")).isNull();
        }
    }

    @NestedPerClass
    @DisplayName("toEntity")
    class ToEntity {

        UUID id = UuidV7.generate();
        ContentItemEntity result;

        @BeforeAll
        void when() {
            ContentItemDTO dto = new ContentItemDTO(id);
            dto.setContentType("Blog");
            dto.add("title", "Hello");

            result = mapper.toEntity(dto);
        }

        @Test
        @DisplayName("it should copy the id")
        void it_should_copy_the_id() {
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("it should copy the contentType")
        void it_should_copy_the_contentType() {
            assertThat(result.getContentType()).isEqualTo("Blog");
        }

        @Test
        @DisplayName("it should copy each schemaless field")
        void it_should_copy_each_schemaless_field() {
            assertThat((String) result.get("title")).isEqualTo("Hello");
        }
    }
}
