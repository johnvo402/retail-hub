package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.inventory.common.InventoryReadPort;
import com.johnvo.retailhub.application.features.inventory.common.InventoryView;
import com.johnvo.retailhub.infrastructure.persistence.jpa.catalog.SortParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaInventoryReadAdapter implements InventoryReadPort {
    private final SpringDataInventoryRepository inventory;

    public JpaInventoryReadAdapter(SpringDataInventoryRepository inventory) {
        this.inventory = inventory;
    }

    @Override
    public Optional<InventoryView> findByProductId(UUID productId) {
        return inventory.findDetailedByProductId(productId).map(JpaInventoryReadAdapter::toView);
    }

    @Override
    public PageResponse<InventoryView> findAll(int page, int size, String sort) {
        Page<InventoryJpaEntity> result = inventory.findAll(PageRequest.of(page, size,
                SortParser.parse(sort, "updatedAt")));
        return new PageResponse<>(result.getContent().stream().map(JpaInventoryReadAdapter::toView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static InventoryView toView(InventoryJpaEntity entity) {
        return new InventoryView(entity.getProductId(), entity.getProduct().getSku(),
                entity.getProduct().getName(), entity.getQuantity(), entity.getVersion(), entity.getUpdatedAt());
    }
}

