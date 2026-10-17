package com.contented.contented.contentitem.rest;

import com.contented.contented.contentitem.exceptions.ContentItemConflictException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContentItemExceptionHandler")
class ContentItemExceptionHandlerTest {

    ContentItemExceptionHandler handler = new ContentItemExceptionHandler();

    @Nested
    @DisplayName("`handleConflict()`")
    class HandleConflict {

        @Nested
        @DisplayName("When a `ContentItemConflictException` is handled")
        class WhenHandlingContentItemConflict {

            ProblemDetail problem;

            @BeforeAll
            void when() {
                problem = handler.handleConflict(new ContentItemConflictException("nothing to publish"));
            }

            @Test
            @DisplayName("It should produce a 409 Conflict problem detail")
            void it_should_produce_a_conflict_problem_detail() {
                assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                assertThat(problem.getTitle()).isEqualTo("Conflicting contentItem request");
                assertThat(problem.getDetail()).isEqualTo("nothing to publish");
            }
        }
    }

    @Nested
    @DisplayName("`handleConcurrentWriteConflict()`")
    class HandleConcurrentWriteConflict {

        @Nested
        @DisplayName("When a `DataIntegrityViolationException` is handled")
        class WhenHandlingIntegrityViolation {

            ProblemDetail problem;

            @BeforeAll
            void when() {
                problem = handler.handleConcurrentWriteConflict(new DataIntegrityViolationException("uq_content_item_working"));
            }

            @Test
            @DisplayName("It should produce a 409 Conflict problem detail")
            void it_should_produce_a_conflict_problem_detail() {
                assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                assertThat(problem.getTitle()).isEqualTo("Conflicting contentItem write");
            }
        }
    }
}
