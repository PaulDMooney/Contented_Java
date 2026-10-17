package com.contented.contented.contentitem;

import com.contented.contented.common.UuidV7;
import com.contented.contented.contentitem.model.ContentItemEntity;
import com.contented.contented.contentitem.model.ContentItemState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

// Exercises the real DB-level enforcement (partial unique indexes) that
// ContentItemExceptionHandler's 409 mapping depends on — a genuine Postgres constraint violation,
// translated by Spring's own exception translation, not a hand-constructed exception.
// The @DataJdbcTest slice picks up JdbcConfig, whose jsonb converters need Jackson's ObjectMapper.
@Tag(INTEGRATION_TESTS)
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureJson
@Testcontainers
@DisplayName("`ContentItemRepository` invariant enforcement")
public class ContentItemRepositoryIT {

    @Container
    static PostgreSQLContainer postgres = postgresContainer();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        startAndRegisterPostgresContainer(postgres, registry);
    }

    @Autowired
    ContentItemRepository contentItemRepository;

    @Nested
    @DisplayName("`save()`")
    class Save {

        @Nested
        @DisplayName("Given a WORKING version already exists for an identifier")
        class GivenAnExistingWorkingVersion {

            UUID identifier = UuidV7.generate();

            @BeforeAll
            void given() {
                contentItemRepository.save(ContentItemEntity.newVersion(UuidV7.generate(), identifier,
                    "SomeType", ContentItemState.WORKING, Instant.now(), Map.of()));
            }

            @Nested
            @DisplayName("When another WORKING version is saved for the same identifier")
            class WhenSavingAnotherWorkingVersion {

                Throwable thrown;

                @BeforeAll
                void when() {
                    var secondWorking = ContentItemEntity.newVersion(UuidV7.generate(), identifier,
                        "SomeType", ContentItemState.WORKING, Instant.now(), Map.of());
                    thrown = catchThrowable(() -> contentItemRepository.save(secondWorking));
                }

                @Test
                @DisplayName("It should reject the save with a `DataIntegrityViolationException`")
                void it_should_reject_the_save() {
                    assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
                }
            }
        }

        @Nested
        @DisplayName("Given a LIVE version already exists for an identifier")
        class GivenAnExistingLiveVersion {

            UUID identifier = UuidV7.generate();

            @BeforeAll
            void given() {
                contentItemRepository.save(ContentItemEntity.newVersion(UuidV7.generate(), identifier,
                    "SomeType", ContentItemState.LIVE, Instant.now(), Map.of()));
            }

            @Nested
            @DisplayName("When another LIVE version is saved for the same identifier")
            class WhenSavingAnotherLiveVersion {

                Throwable thrown;

                @BeforeAll
                void when() {
                    var secondLive = ContentItemEntity.newVersion(UuidV7.generate(), identifier,
                        "SomeType", ContentItemState.LIVE, Instant.now(), Map.of());
                    thrown = catchThrowable(() -> contentItemRepository.save(secondLive));
                }

                @Test
                @DisplayName("It should reject the save with a `DataIntegrityViolationException`")
                void it_should_reject_the_save() {
                    assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
                }
            }
        }
    }
}
