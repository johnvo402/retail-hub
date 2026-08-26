package com.johnvo.retailhub.application.features.ordering.query.getorders;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.cqrs.Query;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;

import java.util.UUID;

public record GetOrdersQuery(UUID actorId, boolean admin, int page, int size)
        implements Query<PageResponse<OrderView>> {
}

