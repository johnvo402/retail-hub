package com.johnvo.retailhub.application.features.catalog;

import com.johnvo.retailhub.application.features.catalog.command.createproduct.CreateProductCommand;
import com.johnvo.retailhub.application.features.catalog.command.createproduct.CreateProductCommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.domain.catalog.Category;
import com.johnvo.retailhub.domain.catalog.CategoryId;
import com.johnvo.retailhub.domain.catalog.CategoryRepository;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductCommandHandlerTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock InventoryRepository inventory;
    @Mock ProductSearchIndex search;

    private CreateProductCommandHandler handler;
    private Category category;

    @BeforeEach
    void setUp() {
        Instant now = Instant.parse("2026-08-26T00:00:00Z");
        category = Category.create(CategoryId.newId(), "Keyboards", "", now);
        handler = new CreateProductCommandHandler(products, categories, inventory, search,
                Clock.fixed(now, ZoneOffset.UTC));
        when(products.findBySku("KEY-1")).thenReturn(Optional.empty());
        when(categories.findById(category.id())).thenReturn(Optional.of(category));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventory.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsProductInventoryAndSearchProjection() {
        var result = handler.handle(new CreateProductCommand("Keyboard", "75% board", "KEY-1",
                new BigDecimal("120.00"), category.id().value()));

        ArgumentCaptor<InventoryItem> inventoryItem = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventory).save(inventoryItem.capture());
        verify(search).index(any());
        assertThat(result.isSuccess()).isTrue();
        assertThat(inventoryItem.getValue().productId().value()).isEqualTo(result.value().id());
        assertThat(inventoryItem.getValue().quantity()).isZero();
    }
}
