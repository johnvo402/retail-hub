package com.johnvo.retailhub.application.features.ordering.command.createorder;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.application.features.ordering.common.OrderConcurrencyException;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, CreatedId> {
    private final OrderCommandSupport orders;
    private final Clock clock;

    public CreateOrderCommandHandler(OrderCommandSupport orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Override
    public Result<CreatedId> handle(CreateOrderCommand command) {
        Order order = Order.create(OrderId.newId(), command.customerId(), clock.instant());
        try {
            Order saved = orders.create(order);
            return Result.success(new CreatedId(saved.id().value()));
        } catch (OrderConcurrencyException exception) {
            return Result.failure(new ApplicationError("ORDER_CONCURRENCY_CONFLICT",
                    "Order was modified by another request; reload and retry", ErrorType.CONFLICT));
        }
    }
}
