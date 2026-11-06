package dev.cormac.huddle.domain;

/** The summariser could not produce a result (not configured, upstream error, or declined). */
public class SummaryUnavailableException extends RuntimeException {

    public SummaryUnavailableException(String message) {
        super(message);
    }

    public SummaryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
