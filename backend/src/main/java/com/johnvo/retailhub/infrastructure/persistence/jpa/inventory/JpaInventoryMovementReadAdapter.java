package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementReadPort;
import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaInventoryMovementReadAdapter implements InventoryMovementReadPort {
    private final SpringDataInventoryMovementRepository movements;

    public JpaInventoryMovementReadAdapter(SpringDataInventoryMovementRepository movements) {
        this.movements = movements;
    }

    @Override
    public PageResponse<InventoryMovementView> findByProductId(UUID productId, int page, int size) {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Page<InventoryMovementJpaEntity> result = movements.findByProductId(productId,
                PageRequest.of(page, size, newestFirst));
        return new PageResponse<>(result.getContent().stream()
                .map(JpaInventoryMovementReadAdapter::toView).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static InventoryMovementView toView(InventoryMovementJpaEntity entity) {
        return new InventoryMovementView(entity.getId(), entity.getProductId(), entity.getType(),
                entity.getQuantityDelta(), entity.getQuantityBefore(), entity.getQuantityAfter(),
                entity.getActorUserId(), entity.getReferenceId(), entity.getReason(), entity.getCreatedAt());
    }
}
