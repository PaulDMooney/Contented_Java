package com.contented.contented.contentlet;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ContentletExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ContentletNotFoundException.class)
    ProblemDetail handleNotFound(ContentletNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Contentlet not found");
        problem.setProperty("contentletId", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(InvalidContentletException.class)
    ProblemDetail handleInvalid(InvalidContentletException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid contentlet request");
        return problem;
    }
}
