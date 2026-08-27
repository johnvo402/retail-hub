package com.johnvo.retailhub.application.features.ordering.command.addorderitem;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.application.features.ordering.common.OrderConcurrencyException;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import org.springframework.stereotype.Service;

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
    public Result<CreatedId> handle(AddOrderItemCommand command) {
        Product product = products.findById(new ProductId(command.productId()))
                .filter(Product::active).orElse(null);
        if (product == null) {
            return Result.failure(new ApplicationError(
                    "ORDER_PRODUCT_NOT_FOUND", "Active product was not found", ErrorType.NOT_FOUND));
        }
        try {
            return orders.updateOwned(command.orderId(), command.actorId(), command.admin(), order -> {
                UUID itemId = order.addItem(product.id().value(), product.name(), product.sku(), product.price(),
                        command.quantity(), clock.instant());
                return new CreatedId(itemId);
            });
        } catch (OrderConcurrencyException exception) {
            return Result.failure(new ApplicationError("ORDER_CONCURRENCY_CONFLICT",
                    "Order was modified by another request; reload and retry", ErrorType.CONFLICT));
        }
    }
}
