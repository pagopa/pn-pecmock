package it.pagopa.pn.pecmock.exception;

public class SemaphoreException extends RuntimeException {

    public SemaphoreException() {
        super("Thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted");
    }
}
