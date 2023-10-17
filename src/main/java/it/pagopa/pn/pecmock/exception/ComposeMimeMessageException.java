package it.pagopa.pn.pecmock.exception;

public class ComposeMimeMessageException extends RuntimeException {

    public ComposeMimeMessageException() {
        super("An error occurred during MIME message composition");
    }
}
