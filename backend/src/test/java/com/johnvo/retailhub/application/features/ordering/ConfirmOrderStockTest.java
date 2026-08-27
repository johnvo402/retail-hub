package com.johnvo.retailhub.application.features.ordering;

import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementRepository;
import com.johnvo.retailhub.application.features.ordering.common.OrderCommandSupport;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryMovement;
import com.johnvo.retailhub.domain.inventory.InventoryMovementType;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import com.johnvo.retailhub.domain.ordering.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderStockTest {
    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");

    @Mock OrderRepository orders;
    @Mock InventoryRepository inventory;
    @Mock InventoryMovementRepository movements;
    private OrderCommandSupport support;

    @BeforeEach
    void setUp() {
        support = new OrderCommandSupport(orders, inventory, movements);
    }

    @Test
    void successfulConfirmationDeductsStockAndPersistsEverything() {
        UUID productId = UUID.randomUUID();
        Order order = orderWithItem(UUID.randomUUID(), productId, "Atlas Keyboard", 3);
        InventoryItem stock = stock(productId, 10);
        given(order, stock);

        var result = support.confirmOwned(order.id().value(), order.customerId(), false, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(stock.quantity()).isEqualTo(7);
        verify(inventory).save(stock);
        verify(orders).save(order);
        InventoryMovement movement = capturedMovements(1).getFirst();
        assertThat(movement.type()).isEqualTo(InventoryMovementType.ORDER_CONFIRMATION);
        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.quantityDelta()).isEqualTo(-3);
        assertThat(movement.quantityAfter()).isEqualTo(7);
        assertThat(movement.actorUserId()).isEqualTo(order.customerId());
        assertThat(movement.referenceId()).isEqualTo(order.id().value());
    }

    @Test
    void insufficientStockLeavesOrderAndInventoryUnchanged() {
        UUID productId = UUID.randomUUID();
        Order order = orderWithItem(UUID.randomUUID(), productId, "Atlas Keyboard", 3);
        InventoryItem stock = stock(productId, 2);
        given(order, stock);

        var result = support.confirmOwned(order.id().value(), order.customerId(), false, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("INVENTORY_INSUFFICIENT_STOCK");
        assertThat(result.error().type()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(result.error().message()).contains("Atlas Keyboard").contains("requested 3").contains("available 2");
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(stock.quantity()).isEqualTo(2);
        verify(inventory, never()).save(stock);
        verify(orders, never()).save(order);
        verify(movements, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void insufficientSecondItemDoesNotDeductFirstItem() {
        UUID customerId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        Order order = Order.create(OrderId.newId(), customerId, NOW.minusSeconds(10));
        order.addItem(firstProductId, "Keyboard", "KEY-1", BigDecimal.TEN, 3, NOW.minusSeconds(5));
        order.addItem(secondProductId, "Mouse", "MOUSE-1", BigDecimal.TEN, 3, NOW.minusSeconds(4));
        InventoryItem firstStock = stock(firstProductId, 10);
        InventoryItem secondStock = stock(secondProductId, 2);
        when(orders.findById(order.id())).thenReturn(Optional.of(order));
        when(inventory.findByProductId(new ProductId(firstProductId))).thenReturn(Optional.of(firstStock));
        when(inventory.findByProductId(new ProductId(secondProductId))).thenReturn(Optional.of(secondStock));

        var result = support.confirmOwned(order.id().value(), customerId, false, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("INVENTORY_INSUFFICIENT_STOCK");
        assertThat(firstStock.quantity()).isEqualTo(10);
        assertThat(secondStock.quantity()).isEqualTo(2);
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        verify(inventory, never()).save(firstStock);
        verify(inventory, never()).save(secondStock);
        verify(orders, never()).save(order);
        verify(movements, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirmingTwiceOnlyDeductsStockOnce() {
        UUID productId = UUID.randomUUID();
        Order order = orderWithItem(UUID.randomUUID(), productId, "Atlas Keyboard", 3);
        InventoryItem stock = stock(productId, 10);
        given(order, stock);

        var first = support.confirmOwned(order.id().value(), order.customerId(), false, NOW);
        var second = support.confirmOwned(order.id().value(), order.customerId(), false, NOW.plusSeconds(1));

        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isFailure()).isTrue();
        assertThat(second.error().code()).isEqualTo("ORDER_INVALID_STATE");
        assertThat(stock.quantity()).isEqualTo(7);
        verify(inventory, times(1)).save(stock);
        verify(orders, times(1)).save(order);
        verify(movements, times(1)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonOwnerCannotConfirmAnotherUsersOrder() {
        Order order = orderWithItem(UUID.randomUUID(), UUID.randomUUID(), "Keyboard", 1);
        when(orders.findById(order.id())).thenReturn(Optional.of(order));

        var result = support.confirmOwned(order.id().value(), UUID.randomUUID(), false, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("ORDER_FORBIDDEN");
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        verifyNoInteractions(inventory);
        verifyNoInteractions(movements);
        verify(orders, never()).save(order);
    }

    @Test
    void adminCanConfirmAnotherUsersOrder() {
        UUID productId = UUID.randomUUID();
        Order order = orderWithItem(UUID.randomUUID(), productId, "Keyboard", 1);
        InventoryItem stock = stock(productId, 2);
        given(order, stock);

        var result = support.confirmOwned(order.id().value(), UUID.randomUUID(), true, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(stock.quantity()).isEqualTo(1);
        verify(inventory).save(stock);
        verify(orders).save(order);
    }

    @Test
    void duplicateProductLinesUseTheirCombinedQuantity() {
        UUID productId = UUID.randomUUID();
        Order order = Order.create(OrderId.newId(), UUID.randomUUID(), NOW.minusSeconds(10));
        order.addItem(productId, "Keyboard", "KEY-1", BigDecimal.TEN, 3, NOW.minusSeconds(5));
        order.addItem(productId, "Keyboard", "KEY-1", BigDecimal.TEN, 3, NOW.minusSeconds(4));
        InventoryItem stock = stock(productId, 5);
        given(order, stock);

        var result = support.confirmOwned(order.id().value(), order.customerId(), false, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("INVENTORY_INSUFFICIENT_STOCK");
        assertThat(result.error().message()).contains("requested 6").contains("available 5");
        assertThat(stock.quantity()).isEqualTo(5);
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        verify(inventory, never()).save(stock);
        verify(orders, never()).save(order);
        verify(movements, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void multiItemOrderCreatesOneMovementForEachAffectedProduct() {
        UUID customerId = UUID.randomUUID();
        UUID keyboardId = UUID.randomUUID();
        UUID mouseId = UUID.randomUUID();
        Order order = Order.create(OrderId.newId(), customerId, NOW.minusSeconds(10));
        order.addItem(keyboardId, "Keyboard", "KEY-1", BigDecimal.TEN, 3, NOW.minusSeconds(5));
        order.addItem(mouseId, "Mouse", "MOUSE-1", BigDecimal.TEN, 2, NOW.minusSeconds(4));
        InventoryItem keyboardStock = stock(keyboardId, 10);
        InventoryItem mouseStock = stock(mouseId, 5);
        when(orders.findById(order.id())).thenReturn(Optional.of(order));
        when(inventory.findByProductId(new ProductId(keyboardId))).thenReturn(Optional.of(keyboardStock));
        when(inventory.findByProductId(new ProductId(mouseId))).thenReturn(Optional.of(mouseStock));

        var result = support.confirmOwned(order.id().value(), customerId, false, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(keyboardStock.quantity()).isEqualTo(7);
        assertThat(mouseStock.quantity()).isEqualTo(3);
        List<InventoryMovement> savedMovements = capturedMovements(2);
        assertThat(savedMovements).extracting(movement -> movement.productId().value())
                .containsExactly(keyboardId, mouseId);
        assertThat(savedMovements).extracting(InventoryMovement::quantityDelta)
                .containsExactly(-3, -2);
    }

    @Test
    void duplicateProductLinesCreateOneAggregatedMovement() {
        UUID productId = UUID.randomUUID();
        Order order = Order.create(OrderId.newId(), UUID.randomUUID(), NOW.minusSeconds(10));
        order.addItem(productId, "Keyboard", "KEY-1", BigDecimal.TEN, 2, NOW.minusSeconds(5));
        order.addItem(productId, "Keyboard", "KEY-1", BigDecimal.TEN, 3, NOW.minusSeconds(4));
        InventoryItem stock = stock(productId, 10);
        given(order, stock);

        var result = support.confirmOwned(order.id().value(), order.customerId(), false, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(stock.quantity()).isEqualTo(5);
        InventoryMovement movement = capturedMovements(1).getFirst();
        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.quantityDelta()).isEqualTo(-5);
        assertThat(movement.quantityAfter()).isEqualTo(5);
    }

    private void given(Order order, InventoryItem stock) {
        when(orders.findById(order.id())).thenReturn(Optional.of(order));
        when(inventory.findByProductId(stock.productId())).thenReturn(Optional.of(stock));
    }

    private List<InventoryMovement> capturedMovements(int count) {
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movements, times(count)).save(captor.capture());
        return captor.getAllValues();
    }

    private static Order orderWithItem(UUID customerId, UUID productId, String productName, int quantity) {
        Order order = Order.create(OrderId.newId(), customerId, NOW.minusSeconds(10));
        order.addItem(productId, productName, "SKU-1", BigDecimal.TEN, quantity, NOW.minusSeconds(5));
        return order;
    }

    private static InventoryItem stock(UUID productId, int quantity) {
        InventoryItem stock = InventoryItem.create(new ProductId(productId), NOW.minusSeconds(10));
        stock.increase(quantity, NOW.minusSeconds(5));
        return stock;
    }
}
