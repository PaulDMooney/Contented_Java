package com.contented.contented.contentitem.elasticsearch.transformation;

import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemState;
import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.transformation.StandardDMSContentTransformer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlogTransformer")
class BlogTransformerTest {

    Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @Nested
    @DisplayName("`transform()`")
    class Transform {

        @Nested
        @DisplayName("When called with a `contentItem` entity that has `Blog` fields")
        class WhenCalledWithBlogContentItem {

            ContentItemEntity toTransform = blogEntity();

            static ContentItemEntity blogEntity() {
                return ContentItemEntity.newVersion(UuidV7.generate(), UuidV7.generate(), "Blog",
                        ContentItemState.WORKING, Instant.now(),
                        Map.ofEntries(
                                entry("title", "Blog Title"),
                                entry("body", "Blog Body"),
                                entry("language", "en")
                        ));
            }

            BlogTransformer blogTransformer = new BlogTransformer();

            Collection<EntityAsMap> result;

            @BeforeAll
            void when() {
                result = blogTransformer.transform(toTransform);
            }

            @Test
            @DisplayName("It should return an EntityMap containing the transformed fields")
            void it_should_return_an_entity_map_with_transformed_fields() {

                assertThat(result)
                    .hasSize(1)
                    .element(0)
                    .satisfies(entityAsMap ->
                        assertThat(entityAsMap)
                            .containsEntry("contentType", "blog")
                            .containsEntry("blog.title", "Blog Title")
                            .containsEntry("language", "en")
                    );
            }

            @Test
            @DisplayName("It should exclude fields not intended for indexing")
            void it_should_return_an_entity_map_without_fields_unintended_for_indexing() {
                var result = blogTransformer.transform(toTransform);

                assertThat(result).element(0)
                    .satisfies(entityAsMap -> assertThat(entityAsMap)
                        .doesNotContainKeys("stName", "title", "body"));
            }

            @Test
            @DisplayName("It should key the document on the version-agnostic identifier and carry the version id")
            void it_should_key_on_identifier_and_carry_version_id() {

                assertThat(result).element(0)
                    .satisfies(entityAsMap -> assertThat(entityAsMap)
                        .containsEntry("id", toTransform.getIdentifier().toString())
                        .containsEntry("versionId", toTransform.getVersionId().toString()));
            }
        }
    }

}