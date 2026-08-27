package com.johnvo.retailhub.domain.ordering;

import com.johnvo.retailhub.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    @Test
    void newOrderStartsDraftAndEmpty() {
        UUID customerId = UUID.randomUUID();
        Order order = Order.create(OrderId.newId(), customerId, NOW);

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.customerId()).isEqualTo(customerId);
        assertThat(order.items()).isEmpty();
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void addsItemToDraftAndCalculatesTotal() {
        Order order = newOrder();

        UUID itemId = order.addItem(UUID.randomUUID(), "Keyboard", "KEY-1",
                new BigDecimal("120.00"), 2, NOW.plusSeconds(1));

        assertThat(order.items()).singleElement().extracting(OrderItem::id).isEqualTo(itemId);
        assertThat(order.totalAmount()).isEqualByComparingTo("240.00");
        assertThat(order.updatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void rejectsInvalidQuantityAndNegativePrice() {
        Order order = newOrder();

        assertThatThrownBy(() -> order.addItem(UUID.randomUUID(), "Keyboard", "KEY-1",
                BigDecimal.TEN, 0, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> order.addItem(UUID.randomUUID(), "Keyboard", "KEY-1",
                new BigDecimal("-0.01"), 1, NOW)).isInstanceOf(DomainException.class);
        assertThat(order.items()).isEmpty();
    }

    @Test
    void rejectsMissingItemRemoval() {
        assertThatThrownBy(() -> newOrder().removeItem(UUID.randomUUID(), NOW))
                .isInstanceOf(DomainException.class)
                .hasMessage("Order item does not exist");
    }

    @Test
    void confirmsPopulatedOrderButRejectsEmptyOrder() {
        Order empty = newOrder();
        assertThatThrownBy(() -> empty.confirm(NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("without items");

        Order populated = orderWithItem();
        populated.confirm(NOW.plusSeconds(2));
        assertThat(populated.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(populated.confirmedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    void confirmedOrderCannotBeModified() {
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
        UUID itemId = order.items().getFirst().id();
        order.cancel(NOW.plusSeconds(1));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancelledAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> order.removeItem(itemId, NOW.plusSeconds(2)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("draft");
    }

    private static Order newOrder() {
        return Order.create(OrderId.newId(), UUID.randomUUID(), NOW);
    }

    private static Order orderWithItem() {
        Order order = newOrder();
        order.addItem(UUID.randomUUID(), "Keyboard", "KEY-1", new BigDecimal("120.00"), 1, NOW);
        return order;
    }
}
