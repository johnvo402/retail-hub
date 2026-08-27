package com.johnvo.retailhub.application.features.ordering.query.getorder;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
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
    public Result<OrderView> handle(GetOrderQuery query) {
        OrderView order = orders.findById(query.orderId()).orElse(null);
        if (order == null) {
            return Result.failure(new ApplicationError(
                    "ORDER_NOT_FOUND", "Order was not found", ErrorType.NOT_FOUND));
        }
        if (!query.admin() && !order.customerId().equals(query.actorId())) {
            return Result.failure(new ApplicationError(
                    "ORDER_FORBIDDEN", "You cannot view this order", ErrorType.FORBIDDEN));
        }
        return Result.success(order);
    }
}
