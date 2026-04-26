package ru.rentplatform.dealpaymentservice.api.exception;

public class DealTimeConflictException extends RuntimeException {

    public DealTimeConflictException(String message) {
        super(message);
    }
}
