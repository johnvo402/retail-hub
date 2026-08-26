package com.johnvo.retailhub.application.features.ordering.query.getorders;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderReadPort;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;
import org.springframework.stereotype.Service;

@Service
public class GetOrdersQueryHandler implements QueryHandler<GetOrdersQuery, PageResponse<OrderView>> {
    private final OrderReadPort orders;

    public GetOrdersQueryHandler(OrderReadPort orders) {
        this.orders = orders;
    }

    @Override
    public PageResponse<OrderView> handle(GetOrdersQuery query) {
        return orders.findAll(query.admin() ? null : query.actorId(), Math.max(query.page(), 0),
                Math.min(Math.max(query.size(), 1), 100));
    }
}

