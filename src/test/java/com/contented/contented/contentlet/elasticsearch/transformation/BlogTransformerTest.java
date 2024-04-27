package com.contented.contented.contentlet.elasticsearch.transformation;

import com.contented.contented.contentlet.ContentletEntity;
import com.contented.contented.contentlet.testutils.NestedPerClass;
import com.contented.contented.contentlet.transformation.StandardDMSContentTransformer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.EntityAsMap;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlogTransformer")
class BlogTransformerTest {

    Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @NestedPerClass
    @DisplayName("transform")
    class Transform {

        @NestedPerClass
        @DisplayName("Given a contentlet entity with Blog fields")
        class GivenBlogContentlet {

            ContentletEntity toTransform = new StandardDMSContentTransformer(clock)
                    .transform(
                            new ContentletEntity("1234",
                                    Map.ofEntries(
                                            entry("stName", "Blog"),
                                            entry("title", "Blog Title"),
                                            entry("body", "Blog Body"),
                                            entry("language", "en")
                                    )
                            ));

            BlogTransformer blogTransformer = new BlogTransformer();

            @NestedPerClass
            @DisplayName("When the contentlet entity is transformed")
            class WhenTransformed {

                ESCrudContainer result;

                @BeforeAll
                void when() {
                    result = blogTransformer.transform(toTransform, null);
                }

                @Test
                @DisplayName("the EntityMap should contain transformed fields")
                void it_should_return_an_entity_map_with_transformed_fields() {

                    assertThat(result).extracting(ESCrudContainer::toSave).asList()
                        .hasSize(1)
                        .element(0)
                        .satisfies(entityAsMap ->
                            assertThat((EntityAsMap) entityAsMap)
                                .containsEntry("contentType", "blog")
                                .containsEntry("blog.title", "Blog Title")
                                .containsEntry("language", "en")
                        );
                }

                @Test
                @DisplayName("the EntityMap should not contain unintended for indexing")
                void it_should_return_an_entity_map_without_fields_unintended_for_indexing() {
                    var result = blogTransformer.transform(toTransform, null);

                    assertThat(result).extracting(ESCrudContainer::toSave).asList().element(0)
                        .satisfies(entityAsMap -> assertThat((EntityAsMap) entityAsMap)
                            .doesNotContainKeys("stName", "title", "body"));
                }

                @Test
                @DisplayName("the EntityMap should contain an identifier constructed from the id and language")
                void it_should_return_an_entity_map_with_an_identifier() {

                    assertThat(result).extracting(ESCrudContainer::toSave).asList().element(0)
                        .satisfies(entityAsMap -> assertThat((EntityAsMap) entityAsMap)
                            .containsEntry("identifier", "1234_en"));
                }
            }


        }
    }

}