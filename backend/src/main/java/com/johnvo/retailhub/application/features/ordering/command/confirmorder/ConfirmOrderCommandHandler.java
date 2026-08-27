package com.johnvo.retailhub.application.features.ordering.command.confirmorder;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.features.inventory.common.InventoryConcurrencyException;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.application.features.ordering.common.OrderConcurrencyException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ConfirmOrderCommandHandler implements CommandHandler<ConfirmOrderCommand, Void> {
    private final OrderCommandSupport orders;
    private final Clock clock;

    public ConfirmOrderCommandHandler(OrderCommandSupport orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ConfirmOrderCommand command) {
        try {
            return orders.confirmOwned(command.orderId(), command.actorId(), command.admin(), clock.instant());
        } catch (InventoryConcurrencyException exception) {
            return Result.failure(new ApplicationError("INVENTORY_CONCURRENCY_CONFLICT",
                    "Inventory was modified by another request; reload and retry", ErrorType.CONFLICT));
        } catch (OrderConcurrencyException exception) {
            return Result.failure(new ApplicationError("ORDER_CONCURRENCY_CONFLICT",
                    "Order was modified by another request; reload and retry", ErrorType.CONFLICT));
        }
    }
}
