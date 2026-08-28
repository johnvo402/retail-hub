package com.johnvo.retailhub.application.features.catalog;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.catalog.common.CatalogReadPort;
import com.johnvo.retailhub.application.features.catalog.common.ProductCache;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.application.features.catalog.query.getproduct.GetProductQuery;
import com.johnvo.retailhub.application.features.catalog.query.getproduct.GetProductQueryHandler;
import com.johnvo.retailhub.application.features.catalog.query.getproducts.GetProductsQuery;
import com.johnvo.retailhub.application.features.catalog.query.getproducts.GetProductsQueryHandler;
import com.johnvo.retailhub.domain.catalog.ProductFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVisibilityQueryHandlersTest {
    @Mock CatalogReadPort catalog;
    @Mock ProductCache cache;

    @Test
    void nonAdminListForcesActiveAndPreservesEveryOtherFilter() {
        ProductFilter requested = new ProductFilter(UUID.randomUUID(), new BigDecimal("10.00"),
                new BigDecimal("50.00"), false, "keyboard", 2, 12, "name,desc");
        when(catalog.findProducts(any()))
                .thenReturn(new PageResponse<>(List.of(), 2, 12, 0, 0));

        new GetProductsQueryHandler(catalog).handle(new GetProductsQuery(requested, false));

        ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
        verify(catalog).findProducts(filter.capture());
        assertThat(filter.getValue()).isEqualTo(new ProductFilter(requested.categoryId(), requested.minPrice(),
                requested.maxPrice(), true, requested.keyword(), requested.page(), requested.size(),
                requested.sort()));
    }

    @Test
    void adminListPreservesAnOmittedActiveFilter() {
        ProductFilter requested = new ProductFilter(null, null, null, null, null, 0, 20,
                "createdAt,desc");
        when(catalog.findProducts(any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        new GetProductsQueryHandler(catalog).handle(new GetProductsQuery(requested, true));

        ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
        verify(catalog).findProducts(filter.capture());
        assertThat(filter.getValue()).isEqualTo(requested);
    }

    @Test
    void cachedInactiveProductIsHiddenFromNonAdminButVisibleToAdmin() {
        UUID productId = UUID.randomUUID();
        ProductView inactive = product(productId, false);
        when(cache.get(productId)).thenReturn(Optional.of(inactive));
        GetProductQueryHandler handler = new GetProductQueryHandler(catalog, cache);

        var publicResult = handler.handle(new GetProductQuery(productId, false));
        var adminResult = handler.handle(new GetProductQuery(productId, true));

        assertThat(publicResult.isFailure()).isTrue();
        assertThat(publicResult.error().code()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(adminResult.isSuccess()).isTrue();
        assertThat(adminResult.value()).isEqualTo(inactive);
        verifyNoInteractions(catalog);
    }

    @Test
    void activeProductLoadedFromPersistenceIsCachedAndVisiblePublicly() {
        UUID productId = UUID.randomUUID();
        ProductView active = product(productId, true);
        when(cache.get(productId)).thenReturn(Optional.empty());
        when(catalog.findProduct(productId)).thenReturn(Optional.of(active));

        var result = new GetProductQueryHandler(catalog, cache)
                .handle(new GetProductQuery(productId, false));

        assertThat(result.isSuccess()).isTrue();
        verify(cache).put(active);
    }

    private static ProductView product(UUID id, boolean active) {
        Instant now = Instant.parse("2026-08-28T00:00:00Z");
        return new ProductView(id, "Keyboard", "Mechanical keyboard", "KEY-1",
                new BigDecimal("120.00"), UUID.randomUUID(), "Keyboards", active, now, now);
    }
}
