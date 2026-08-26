package com.johnvo.retailhub.domain.ordering;

import com.johnvo.retailhub.domain.ordering.events.OrderConfirmed;
import com.johnvo.retailhub.domain.ordering.events.OrderCreated;
import com.johnvo.retailhub.domain.ordering.events.OrderEvent;
import com.johnvo.retailhub.domain.ordering.events.OrderItemAdded;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    @Test
    void createOrderEmitsOrderCreated() {
        UUID customerId = UUID.randomUUID();
        Order order = Order.create(OrderId.newId(), customerId, NOW);

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.customerId()).isEqualTo(customerId);
        assertThat(order.getVersion()).isEqualTo(1);
        assertThat(order.getUncommittedEvents()).singleElement().isInstanceOf(OrderCreated.class);
    }

    @Test
    void cannotConfirmEmptyOrder() {
        Order order = Order.create(OrderId.newId(), UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> order.confirm(NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("without items");
    }

    @Test
    void cannotModifyConfirmedOrder() {
        Order order = orderWithItem();
        order.confirm(NOW.plusSeconds(1));

        assertThatThrownBy(() -> order.addItem(UUID.randomUUID(), "Mouse", "MOUSE-1",
                BigDecimal.TEN, 1, NOW.plusSeconds(2)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void cancelledOrderCannotBeModified() {
        Order order = orderWithItem();
        order.cancel(NOW.plusSeconds(1));

        assertThatThrownBy(() -> order.removeItem(order.items().getFirst().id(), NOW.plusSeconds(2)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void aggregateRehydratesFromHistoryWithoutUncommittedEvents() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        List<OrderEvent> history = List.of(
                new OrderCreated(orderId, customerId, NOW),
                new OrderItemAdded(orderId, itemId, UUID.randomUUID(), "Keyboard", "KEY-1",
                        new BigDecimal("120.00"), 2, NOW.plusSeconds(1)),
                new OrderConfirmed(orderId, NOW.plusSeconds(2))
        );

        Order order = Order.rehydrate(history);

        assertThat(order.id().value()).isEqualTo(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.totalAmount()).isEqualByComparingTo("240.00");
        assertThat(order.getVersion()).isEqualTo(3);
        assertThat(order.getUncommittedEvents()).isEmpty();
    }

    private static Order orderWithItem() {
        Order order = Order.create(OrderId.newId(), UUID.randomUUID(), NOW);
        order.addItem(UUID.randomUUID(), "Keyboard", "KEY-1", new BigDecimal("120.00"), 1, NOW);
        return order;
    }
}

