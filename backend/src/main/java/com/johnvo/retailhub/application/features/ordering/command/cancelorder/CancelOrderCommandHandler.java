package com.johnvo.retailhub.application.features.ordering.command.cancelorder;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.ordering.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Void handle(CancelOrderCommand command) {
        Order order = orders.loadOwned(command.orderId(), command.actorId(), command.admin());
        long expectedVersion = order.getVersion();
        order.cancel(clock.instant());
        orders.persist(order, expectedVersion);
        return null;
    }
}

