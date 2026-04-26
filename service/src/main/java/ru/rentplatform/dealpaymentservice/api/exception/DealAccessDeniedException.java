package ru.rentplatform.dealpaymentservice.api.exception;

public class DealAccessDeniedException extends RuntimeException {

    public DealAccessDeniedException(String message) {
        super(message);
    }
}
