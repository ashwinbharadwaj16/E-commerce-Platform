package com.microservices.InventoryService.repository;


import com.microservices.InventoryService.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findBySkuCodeIn(List<String> skuCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findBySkuCode(String skuCode);
}