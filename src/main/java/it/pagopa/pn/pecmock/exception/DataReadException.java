package it.pagopa.pn.pecmock.exception;

public class DataReadException extends RuntimeException {
    public DataReadException(String message) {
        super("Exception while reading data from input : " + message);
    }
}
