package com.johnvo.retailhub.application.features.ordering.query.getorder;

import com.johnvo.retailhub.application.common.ForbiddenException;
import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.QueryHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderReadPort;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;
import org.springframework.stereotype.Service;

@Service
public class GetOrderQueryHandler implements QueryHandler<GetOrderQuery, OrderView> {
    private final OrderReadPort orders;

    public GetOrderQueryHandler(OrderReadPort orders) {
        this.orders = orders;
    }

    @Override
    public OrderView handle(GetOrderQuery query) {
        OrderView order = orders.findById(query.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found"));
        if (!query.admin() && !order.customerId().equals(query.actorId())) {
            throw new ForbiddenException("You cannot view this order");
        }
        return order;
    }
}

