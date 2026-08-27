package com.johnvo.retailhub.application.features.ordering.common;

public final class OrderConcurrencyException extends RuntimeException {
    public OrderConcurrencyException(Throwable cause) {
        super("Order was modified by another request", cause);
    }
}
