package com.johnvo.retailhub.domain.ordering;

import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(OrderId id);

    Order save(Order order);
}
