package com.johnvo.retailhub.infrastructure.persistence.jpa.ordering;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.ordering.common.OrderItemView;
import com.johnvo.retailhub.application.features.ordering.common.OrderReadPort;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaOrderReadAdapter implements OrderReadPort {
    private final SpringDataOrderReadRepository repository;

    public JpaOrderReadAdapter(SpringDataOrderReadRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<OrderView> findById(UUID orderId) {
        return repository.findDetailedById(orderId).map(entity -> toView(entity, true));
    }

    @Override
    public PageResponse<OrderView> findAll(UUID customerId, int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderReadModelJpaEntity> result = customerId == null
                ? repository.findAll(request)
                : repository.findAllByCustomerId(customerId, request);
        return new PageResponse<>(result.getContent().stream().map(entity -> toView(entity, false)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static OrderView toView(OrderReadModelJpaEntity entity, boolean includeItems) {
        List<OrderItemView> items = includeItems ? entity.getItems().stream()
                .map(item -> new OrderItemView(item.getId(), item.getProductId(), item.getProductName(),
                        item.getSku(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))
                .toList() : List.of();
        return new OrderView(entity.getId(), entity.getCustomerId(), entity.getStatus(), entity.getTotalAmount(),
                entity.getItemCount(), entity.getCreatedAt(), entity.getConfirmedAt(), entity.getCancelledAt(), items);
    }
}

