package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Function;

@Component
public class OrderCommandSupport {
    private final OrderRepository repository;

    public OrderCommandSupport(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Order create(Order order) {
        return repository.save(order);
    }

    @Transactional
    public <T> Result<T> updateOwned(UUID orderId, UUID actorId, boolean admin,
                                     Function<Order, T> operation) {
        Order order = repository.findById(new OrderId(orderId)).orElse(null);
        if (order == null) {
            return Result.failure(new ApplicationError(
                    "ORDER_NOT_FOUND", "Order was not found", ErrorType.NOT_FOUND));
        }
        if (!admin && !order.customerId().equals(actorId)) {
            return Result.failure(new ApplicationError(
                    "ORDER_FORBIDDEN", "You cannot modify this order", ErrorType.FORBIDDEN));
        }
        try {
            T value = operation.apply(order);
            repository.save(order);
            return Result.success(value);
        } catch (DomainException exception) {
            return Result.failure(domainFailure(exception));
        }
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
}
