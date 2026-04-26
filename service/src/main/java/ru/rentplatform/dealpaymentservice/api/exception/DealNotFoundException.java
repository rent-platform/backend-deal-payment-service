package ru.rentplatform.dealpaymentservice.api.exception;

public class DealNotFoundException extends RuntimeException {

    public DealNotFoundException(String message) {
        super(message);
    }
}
