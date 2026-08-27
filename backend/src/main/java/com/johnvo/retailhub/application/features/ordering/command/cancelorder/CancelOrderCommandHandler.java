package com.johnvo.retailhub.application.features.ordering.command.cancelorder;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.application.features.ordering.common.OrderConcurrencyException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class CancelOrderCommandHandler implements CommandHandler<CancelOrderCommand, Void> {
    private final OrderCommandSupport orders;
    private final Clock clock;

    public CancelOrderCommandHandler(OrderCommandSupport orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(CancelOrderCommand command) {
        try {
            return orders.updateOwned(command.orderId(), command.actorId(), command.admin(), order -> {
                order.cancel(clock.instant());
                return null;
            });
        } catch (OrderConcurrencyException exception) {
            return Result.failure(new ApplicationError("ORDER_CONCURRENCY_CONFLICT",
                    "Order was modified by another request; reload and retry", ErrorType.CONFLICT));
        }
    }
}
