package com.johnvo.retailhub.application.features.ordering.query.getorder;

import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;

import java.util.UUID;

public record GetOrderQuery(UUID orderId, UUID actorId, boolean admin) implements Query<OrderView> {
}

