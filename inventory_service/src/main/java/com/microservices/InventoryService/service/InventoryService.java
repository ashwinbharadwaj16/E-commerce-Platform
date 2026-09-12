package com.microservices.InventoryService.service;

import com.microservices.InventoryService.dto.InventoryRequest;
import com.microservices.InventoryService.dto.InventoryResponse;
import com.microservices.InventoryService.model.Inventory;
import com.microservices.InventoryService.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getStock(List<String> skuCode) {
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory ->
                        InventoryResponse.builder()
                                .skuCode(inventory.getSkuCode())
                                .quantity(inventory.getQuantity())
                                .build()
                ).toList();
    }

    @Transactional
    public boolean checkAndReduceStock(List<InventoryRequest> requests) {
        for (InventoryRequest request : requests) {
            Inventory inventory = inventoryRepository.findBySkuCode(request.getSkuCode())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getSkuCode()));

            if (inventory.getQuantity() < request.getQuantity()) {
                return false; // Not enough stock for this product
            }
        }

        // If all products have enough stock, reduce quantities
        for (InventoryRequest request : requests) {
            Inventory inventory = inventoryRepository.findBySkuCode(request.getSkuCode()).get();
            inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
            inventoryRepository.save(inventory);
        }

        return true;
    }
}
