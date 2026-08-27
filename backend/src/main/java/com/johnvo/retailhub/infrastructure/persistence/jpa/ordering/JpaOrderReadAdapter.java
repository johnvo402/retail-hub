package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.ordering.common.OrderItemView;
import com.johnvo.retailhub.application.features.ordering.common.OrderReadPort;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaOrderReadAdapter implements OrderReadPort {
    private final SpringDataOrderRepository repository;

    public JpaOrderReadAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OrderView> findById(UUID orderId) {
        return repository.findDetailedById(orderId).map(entity -> toView(entity, true));
    }

    @Override
    public PageResponse<OrderView> findAll(UUID customerId, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        Page<OrderSummaryProjection> result = customerId == null
                ? repository.findAllSummaries(request)
                : repository.findAllSummariesByCustomerId(customerId, request);
        return new PageResponse<>(result.getContent().stream().map(JpaOrderReadAdapter::toSummaryView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static OrderView toView(OrderJpaEntity entity, boolean includeItems) {
        List<OrderItemView> items = includeItems ? entity.getItems().stream()
                .map(item -> new OrderItemView(item.getId(), item.getProductId(), item.getProductName(),
                        item.getSku(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))
                .toList() : List.of();
        java.math.BigDecimal totalAmount = entity.getItems().stream()
                .map(OrderItemJpaEntity::getLineTotal)
                .reduce(java.math.BigDecimal.ZERO.setScale(2), java.math.BigDecimal::add);
        int itemCount = entity.getItems().stream().mapToInt(OrderItemJpaEntity::getQuantity).sum();
        return new OrderView(entity.getId(), entity.getCustomerId(), entity.getStatus(), totalAmount,
                itemCount, entity.getCreatedAt(), entity.getConfirmedAt(), entity.getCancelledAt(), items);
    }

    private static OrderView toSummaryView(OrderSummaryProjection summary) {
        return new OrderView(summary.getId(), summary.getCustomerId(),
                com.johnvo.retailhub.domain.ordering.OrderStatus.valueOf(summary.getStatus()),
                summary.getTotalAmount(), Math.toIntExact(summary.getItemCount()), summary.getCreatedAt(),
                summary.getConfirmedAt(), summary.getCancelledAt(), List.of());
    }
}
