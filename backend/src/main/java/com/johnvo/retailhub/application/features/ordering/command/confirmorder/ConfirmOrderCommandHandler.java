package com.johnvo.retailhub.application.features.ordering.command.confirmorder;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.ordering.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Void handle(ConfirmOrderCommand command) {
        Order order = orders.loadOwned(command.orderId(), command.actorId(), command.admin());
        long expectedVersion = order.getVersion();
        order.confirm(clock.instant());
        orders.persist(order, expectedVersion);
        return null;
    }
}

