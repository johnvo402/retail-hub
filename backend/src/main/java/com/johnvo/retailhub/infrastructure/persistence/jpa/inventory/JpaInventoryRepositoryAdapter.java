package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import com.johnvo.retailhub.application.features.inventory.common.InventoryConcurrencyException;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.inventory.InventoryItem;
import com.johnvo.retailhub.domain.inventory.InventoryRepository;
import com.johnvo.retailhub.infrastructure.persistence.jpa.catalog.SpringDataProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaInventoryRepositoryAdapter implements InventoryRepository {
    private final SpringDataInventoryRepository inventory;
    private final SpringDataProductRepository products;

    public JpaInventoryRepositoryAdapter(SpringDataInventoryRepository inventory,
                                         SpringDataProductRepository products) {
        this.inventory = inventory;
        this.products = products;
    }

    @Override
    public Optional<InventoryItem> findByProductId(ProductId productId) {
        return inventory.findById(productId.value()).map(JpaInventoryRepositoryAdapter::toDomain);
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryJpaEntity entity = inventory.findById(item.productId().value()).orElseGet(() ->
                new InventoryJpaEntity(products.getReferenceById(item.productId().value()),
                        item.quantity(), item.updatedAt()));
        entity.update(item.quantity(), item.updatedAt());
        try {
            return toDomain(inventory.saveAndFlush(entity));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
            throw new InventoryConcurrencyException(exception);
        }
    }

    private static InventoryItem toDomain(InventoryJpaEntity entity) {
        return InventoryItem.reconstitute(new ProductId(entity.getProductId()), entity.getQuantity(),
                entity.getVersion(), entity.getUpdatedAt());
    }
}
