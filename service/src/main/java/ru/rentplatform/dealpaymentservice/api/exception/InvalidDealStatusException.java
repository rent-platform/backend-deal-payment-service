package ru.rentplatform.dealpaymentservice.api.exception;

public class InvalidDealStatusException extends RuntimeException {

    public InvalidDealStatusException(String message) {
        super(message);
    }
}
