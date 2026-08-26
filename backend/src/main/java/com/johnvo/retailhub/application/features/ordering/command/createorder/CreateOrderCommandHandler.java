package com.johnvo.retailhub.application.features.ordering.command.createorder;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public CreatedId handle(CreateOrderCommand command) {
        Order order = Order.create(OrderId.newId(), command.customerId(), clock.instant());
        orders.persist(order, 0);
        return new CreatedId(order.id().value());
    }
}

