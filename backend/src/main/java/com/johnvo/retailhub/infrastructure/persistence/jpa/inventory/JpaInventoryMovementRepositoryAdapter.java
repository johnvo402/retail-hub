package com.johnvo.retailhub.infrastructure.persistence.jpa.inventory;

import com.johnvo.retailhub.application.features.inventory.common.InventoryMovementRepository;
import com.johnvo.retailhub.domain.inventory.InventoryMovement;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInventoryMovementRepositoryAdapter implements InventoryMovementRepository {
    private final SpringDataInventoryMovementRepository movements;

    public JpaInventoryMovementRepositoryAdapter(SpringDataInventoryMovementRepository movements) {
        this.movements = movements;
    }

    @Override
    public void save(InventoryMovement movement) {
        movements.save(new InventoryMovementJpaEntity(movement));
    }
}
