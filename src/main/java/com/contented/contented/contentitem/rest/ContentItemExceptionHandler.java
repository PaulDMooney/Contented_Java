package com.contented.contented.contentitem.rest;

import com.contented.contented.contentitem.exceptions.ContentItemNotFoundException;
import com.contented.contented.contentitem.exceptions.InvalidContentItemException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ContentItemExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ContentItemNotFoundException.class)
    ProblemDetail handleNotFound(ContentItemNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("ContentItem not found");
        problem.setProperty("contentItemId", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(InvalidContentItemException.class)
    ProblemDetail handleInvalid(InvalidContentItemException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid contentItem request");
        return problem;
    }

    // The one-LIVE / one-WORKING-per-identifier invariants are enforced by partial unique indexes.
    // Concurrent writes to the same content can race past the service's read-then-write and collide at
    // commit; the loser surfaces here as a conflict rather than a generic 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConflict(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The contentItem was modified concurrently; please retry.");
        problem.setTitle("Conflicting contentItem write");
        return problem;
    }
}
