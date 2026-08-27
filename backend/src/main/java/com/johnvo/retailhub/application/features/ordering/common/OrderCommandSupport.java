package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.OrderItem;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class OrderCommandSupport {
    private final OrderRepository repository;
    private final InventoryRepository inventory;

    public OrderCommandSupport(OrderRepository repository, InventoryRepository inventory) {
        this.repository = repository;
        this.inventory = inventory;
    }

    @Transactional
    public Order create(Order order) {
        return repository.save(order);
    }

    @Transactional
    public <T> Result<T> updateOwned(UUID orderId, UUID actorId, boolean admin,
                                     Function<Order, T> operation) {
        Result<Order> owned = findOwned(orderId, actorId, admin);
        if (owned.isFailure()) {
            return Result.failure(owned.error());
        }
        Order order = owned.value();
        try {
            T value = operation.apply(order);
            repository.save(order);
            return Result.success(value);
        } catch (DomainException exception) {
            return Result.failure(domainFailure(exception));
        }
    }

    @Transactional
    public Result<Void> confirmOwned(UUID orderId, UUID actorId, boolean admin, Instant now) {
        Result<Order> owned = findOwned(orderId, actorId, admin);
        if (owned.isFailure()) {
            return Result.failure(owned.error());
        }
        Order order = owned.value();
        try {
            order.validateConfirmation();
        } catch (DomainException exception) {
            return Result.failure(domainFailure(exception));
        }

        Map<UUID, StockRequirement> requirements = stockRequirements(order.items());
        List<StockDeduction> deductions = new ArrayList<>(requirements.size());
        for (StockRequirement requirement : requirements.values()) {
            InventoryItem item = inventory.findByProductId(new ProductId(requirement.productId())).orElse(null);
            if (item == null) {
                return Result.failure(new ApplicationError("INVENTORY_NOT_FOUND",
                        "Inventory is unavailable for product '%s'".formatted(requirement.productName()),
                        ErrorType.BUSINESS_RULE));
            }
            if (item.quantity() < requirement.quantity()) {
                return Result.failure(new ApplicationError("INVENTORY_INSUFFICIENT_STOCK",
                        "Insufficient stock for product '%s': requested %d, available %d".formatted(
                                requirement.productName(), requirement.quantity(), item.quantity()),
                        ErrorType.BUSINESS_RULE));
            }
            deductions.add(new StockDeduction(item, requirement.quantity()));
        }

        order.confirm(now);
        deductions.forEach(deduction -> deduction.item().decrease(deduction.quantity(), now));
        deductions.forEach(deduction -> inventory.save(deduction.item()));
        repository.save(order);
        return Result.success();
    }

    private Result<Order> findOwned(UUID orderId, UUID actorId, boolean admin) {
        Order order = repository.findById(new OrderId(orderId)).orElse(null);
        if (order == null) {
            return Result.failure(new ApplicationError(
                    "ORDER_NOT_FOUND", "Order was not found", ErrorType.NOT_FOUND));
        }
        if (!admin && !order.customerId().equals(actorId)) {
            return Result.failure(new ApplicationError(
                    "ORDER_FORBIDDEN", "You cannot modify this order", ErrorType.FORBIDDEN));
        }
        return Result.success(order);
    }

    private static Map<UUID, StockRequirement> stockRequirements(List<OrderItem> items) {
        Map<UUID, StockRequirement> requirements = new LinkedHashMap<>();
        for (OrderItem item : items) {
            requirements.compute(item.productId(), (productId, current) -> current == null
                    ? new StockRequirement(productId, item.productName(), item.quantity())
                    : current.add(item.quantity()));
        }
        return requirements;
    }

    private static ApplicationError domainFailure(DomainException exception) {
        String message = exception.getMessage();
        if ("Order item does not exist".equals(message)) {
            return new ApplicationError("ORDER_ITEM_NOT_FOUND", message, ErrorType.BUSINESS_RULE);
        }
        if ("Cannot confirm an order without items".equals(message)) {
            return new ApplicationError("ORDER_EMPTY", message, ErrorType.BUSINESS_RULE);
        }
        return new ApplicationError("ORDER_INVALID_STATE", message, ErrorType.BUSINESS_RULE);
    }

    private record StockRequirement(UUID productId, String productName, int quantity) {
        StockRequirement add(int additionalQuantity) {
            return new StockRequirement(productId, productName, Math.addExact(quantity, additionalQuantity));
        }
    }

    private record StockDeduction(InventoryItem item, int quantity) {
    }
}
