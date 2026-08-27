package com.johnvo.retailhub.application.features.ordering;

import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import com.johnvo.retailhub.domain.ordering.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCommandSupportTest {
    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    @Mock OrderRepository repository;
    private OrderCommandSupport support;
    private Order order;

    @BeforeEach
    void setUp() {
        support = new OrderCommandSupport(repository);
        order = Order.create(OrderId.newId(), UUID.randomUUID(), NOW);
    }

    @Test
    void missingOrderReturnsNotFoundWithoutWriting() {
        when(repository.findById(order.id())).thenReturn(Optional.empty());

        var result = support.updateOwned(order.id().value(), order.customerId(), false, ignored -> null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("ORDER_NOT_FOUND");
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonOwnerReturnsForbiddenWithoutWriting() {
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        var result = support.updateOwned(order.id().value(), UUID.randomUUID(), false, ignored -> null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().type()).isEqualTo(ErrorType.FORBIDDEN);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidDomainTransitionReturnsBusinessRuleWithoutWriting() {
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        var result = support.updateOwned(order.id().value(), order.customerId(), false, current -> {
            current.confirm(NOW.plusSeconds(1));
            return null;
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("ORDER_EMPTY");
        assertThat(result.error().type()).isEqualTo(ErrorType.BUSINESS_RULE);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void successfulMutationPersistsAggregate() {
        when(repository.findById(order.id())).thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        var result = support.updateOwned(order.id().value(), order.customerId(), false, current -> {
            current.cancel(NOW.plusSeconds(1));
            return null;
        });

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(repository).save(order);
    }
}
