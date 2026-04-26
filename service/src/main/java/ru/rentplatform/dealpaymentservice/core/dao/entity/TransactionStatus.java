package ru.rentplatform.dealpaymentservice.core.dao.entity;

public enum TransactionStatus {
    PENDING,
    HELD,
    CAPTURED,
    REFUNDED,
    FAILED,
    CANCELLED
}
