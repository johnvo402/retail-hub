package com.johnvo.retailhub.application.features.ordering.common;

import com.johnvo.retailhub.application.common.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface OrderReadPort {
    Optional<OrderView> findById(UUID orderId);

    PageResponse<OrderView> findAll(UUID customerId, int page, int size);
}

