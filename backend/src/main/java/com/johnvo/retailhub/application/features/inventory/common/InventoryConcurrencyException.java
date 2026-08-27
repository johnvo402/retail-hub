package com.johnvo.retailhub.application.features.inventory.common;

public final class InventoryConcurrencyException extends RuntimeException {
    public InventoryConcurrencyException(Throwable cause) {
        super("Inventory was modified by another request", cause);
    }
}
