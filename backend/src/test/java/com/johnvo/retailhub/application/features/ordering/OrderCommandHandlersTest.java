package com.johnvo.retailhub.application.features.ordering;

import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.features.ordering.command.confirmorder.ConfirmOrderCommand;
import com.johnvo.retailhub.application.features.ordering.command.confirmorder.ConfirmOrderCommandHandler;
import com.johnvo.retailhub.application.features.ordering.command.createorder.CreateOrderCommand;
import com.johnvo.retailhub.application.features.ordering.command.createorder.CreateOrderCommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.application.features.ordering.common.OrderConcurrencyException;
import com.johnvo.retailhub.domain.ordering.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCommandHandlersTest {
    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock OrderCommandSupport support;

    @Test
    void createReturnsSuccessAndPersistsOrder() {
        when(support.create(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderCommandHandler handler = new CreateOrderCommandHandler(support, CLOCK);

        var result = handler.handle(new CreateOrderCommand(UUID.randomUUID()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isNotNull();
        verify(support).create(any(Order.class));
    }

    @Test
    void concurrentWriteBecomesConflictResult() {
        doThrow(new OrderConcurrencyException(new IllegalStateException("stale")))
                .when(support).updateOwned(any(), any(), anyBoolean(), any());
        ConfirmOrderCommandHandler handler = new ConfirmOrderCommandHandler(support, CLOCK);

        var result = handler.handle(new ConfirmOrderCommand(UUID.randomUUID(), UUID.randomUUID(), false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("ORDER_CONCURRENCY_CONFLICT");
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
    }
}
