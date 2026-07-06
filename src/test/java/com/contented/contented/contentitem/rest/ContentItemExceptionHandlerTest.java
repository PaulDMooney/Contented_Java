package com.contented.contented.contentitem.rest;

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
        @DisplayName("When a `DataIntegrityViolationException` is handled")
        class WhenHandlingIntegrityViolation {

            ProblemDetail problem;

            @BeforeAll
            void when() {
                problem = handler.handleConflict(new DataIntegrityViolationException("uq_content_item_working"));
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
