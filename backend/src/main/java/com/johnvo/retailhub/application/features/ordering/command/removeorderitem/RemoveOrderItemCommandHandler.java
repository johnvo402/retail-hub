package com.johnvo.retailhub.application.features.ordering.command.removeorderitem;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.ordering.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class RemoveOrderItemCommandHandler implements CommandHandler<RemoveOrderItemCommand, Void> {
    private final OrderCommandSupport orders;
    private final Clock clock;

    public RemoveOrderItemCommandHandler(OrderCommandSupport orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Void handle(RemoveOrderItemCommand command) {
        Order order = orders.loadOwned(command.orderId(), command.actorId(), command.admin());
        long expectedVersion = order.getVersion();
        order.removeItem(command.itemId(), clock.instant());
        orders.persist(order, expectedVersion);
        return null;
    }
}

