package com.johnvo.retailhub.domain.ordering;

import com.johnvo.retailhub.domain.shared.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity
) {
    public OrderItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(productId);
        if (productName == null || productName.isBlank()) {
            throw new DomainException("Product name is required");
        }
        if (sku == null || sku.isBlank()) {
            throw new DomainException("SKU is required");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new DomainException("Unit price cannot be negative");
        }
        if (quantity <= 0) {
            throw new DomainException("Order item quantity must be greater than zero");
        }
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}

