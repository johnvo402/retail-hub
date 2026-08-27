package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import com.johnvo.retailhub.application.features.ordering.common.OrderConcurrencyException;
import com.johnvo.retailhub.domain.ordering.Order;
import com.johnvo.retailhub.domain.ordering.OrderId;
import com.johnvo.retailhub.domain.ordering.OrderItem;
import com.johnvo.retailhub.domain.ordering.OrderRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {
    private final SpringDataOrderRepository repository;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return repository.findDetailedById(id.value()).map(JpaOrderRepositoryAdapter::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = repository.findDetailedById(order.id().value())
                .orElseGet(() -> new OrderJpaEntity(order));
        entity.update(order);
        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
            throw new OrderConcurrencyException(exception);
        }
    }

    static Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(new OrderId(entity.getId()), entity.getCustomerId(), entity.getStatus(),
                entity.getItems().stream().map(item -> new OrderItem(item.getId(), item.getProductId(),
                        item.getProductName(), item.getSku(), item.getUnitPrice(), item.getQuantity())).toList(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getConfirmedAt(), entity.getCancelledAt());
    }
}
