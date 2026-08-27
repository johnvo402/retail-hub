package com.johnvo.retailhub.application.features.inventory;

import com.johnvo.retailhub.application.features.inventory.command.decreasestock.DecreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.decreasestock.DecreaseStockCommandHandler;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommandHandler;
import com.johnvo.retailhub.application.features.inventory.common.InventoryConcurrencyException;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementRepository;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryMovement;
import com.johnvo.retailhub.domain.inventory.InventoryMovementType;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAdjustmentCommandHandlerTest {
    private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");

    @Mock InventoryRepository inventory;
    @Mock InventoryMovementRepository movements;
    private IncreaseStockCommandHandler increase;
    private DecreaseStockCommandHandler decrease;
    private UUID productId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        increase = new IncreaseStockCommandHandler(inventory, movements, clock);
        decrease = new DecreaseStockCommandHandler(inventory, movements, clock);
        productId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Test
    void manualIncreaseCreatesBalancedMovement() {
        InventoryItem stock = stock(10);
        when(inventory.findByProductId(stock.productId())).thenReturn(Optional.of(stock));
        when(inventory.save(stock)).thenReturn(stock);

        var result = increase.handle(new IncreaseStockCommand(productId, 5, actorId,
                "  New supplier delivery  "));

        assertThat(result.isSuccess()).isTrue();
        assertThat(stock.quantity()).isEqualTo(15);
        InventoryMovement movement = capturedMovement();
        assertThat(movement.type()).isEqualTo(InventoryMovementType.MANUAL_INCREASE);
        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.quantityDelta()).isEqualTo(5);
        assertThat(movement.quantityAfter()).isEqualTo(15);
        assertThat(movement.actorUserId()).isEqualTo(actorId);
        assertThat(movement.reason()).isEqualTo("New supplier delivery");
    }

    @Test
    void manualDecreaseCreatesBalancedMovement() {
        InventoryItem stock = stock(10);
        when(inventory.findByProductId(stock.productId())).thenReturn(Optional.of(stock));
        when(inventory.save(stock)).thenReturn(stock);

        var result = decrease.handle(new DecreaseStockCommand(productId, 3, actorId, "Damaged stock"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(stock.quantity()).isEqualTo(7);
        InventoryMovement movement = capturedMovement();
        assertThat(movement.type()).isEqualTo(InventoryMovementType.MANUAL_DECREASE);
        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.quantityDelta()).isEqualTo(-3);
        assertThat(movement.quantityAfter()).isEqualTo(7);
    }

    @Test
    void insufficientDecreaseDoesNotPersistInventoryOrMovement() {
        InventoryItem stock = stock(2);
        when(inventory.findByProductId(stock.productId())).thenReturn(Optional.of(stock));

        var result = decrease.handle(new DecreaseStockCommand(productId, 3, actorId, null));

        assertThat(result.isFailure()).isTrue();
        assertThat(stock.quantity()).isEqualTo(2);
        verify(inventory, never()).save(stock);
        verify(movements, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void concurrencyConflictDoesNotAttemptToPersistMovement() {
        InventoryItem stock = stock(10);
        when(inventory.findByProductId(stock.productId())).thenReturn(Optional.of(stock));
        when(inventory.save(stock)).thenThrow(new InventoryConcurrencyException(new RuntimeException()));

        assertThatThrownBy(() -> increase.handle(new IncreaseStockCommand(productId, 1, actorId, null)))
                .isInstanceOf(InventoryConcurrencyException.class);

        verify(movements, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private InventoryMovement capturedMovement() {
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movements).save(captor.capture());
        return captor.getValue();
    }

    private InventoryItem stock(int quantity) {
        InventoryItem stock = InventoryItem.create(new ProductId(productId), NOW.minusSeconds(10));
        stock.increase(quantity, NOW.minusSeconds(5));
        return stock;
    }
}
