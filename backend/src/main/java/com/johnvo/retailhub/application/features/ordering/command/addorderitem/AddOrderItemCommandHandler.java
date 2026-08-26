package com.johnvo.retailhub.application.features.ordering.command.addorderitem;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import com.johnvo.retailhub.domain.ordering.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AddOrderItemCommandHandler implements CommandHandler<AddOrderItemCommand, CreatedId> {
    private final OrderCommandSupport orders;
    private final ProductRepository products;
    private final Clock clock;

    public AddOrderItemCommandHandler(OrderCommandSupport orders, ProductRepository products, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedId handle(AddOrderItemCommand command) {
        Product product = products.findById(new ProductId(command.productId()))
                .filter(Product::active)
                .orElseThrow(() -> new ResourceNotFoundException("Active product was not found"));
        Order order = orders.loadOwned(command.orderId(), command.actorId(), command.admin());
        long expectedVersion = order.getVersion();
        UUID itemId = order.addItem(product.id().value(), product.name(), product.sku(), product.price(),
                command.quantity(), clock.instant());
        orders.persist(order, expectedVersion);
        return new CreatedId(itemId);
    }
}

