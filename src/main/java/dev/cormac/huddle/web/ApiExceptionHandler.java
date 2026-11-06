package dev.cormac.huddle.web;

import dev.cormac.huddle.domain.SummaryUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain validation failures into RFC 9457 problem responses,
 * so controllers stay free of try/catch blocks.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidInput(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SummaryUnavailableException.class)
    ProblemDetail handleSummaryUnavailable(SummaryUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
