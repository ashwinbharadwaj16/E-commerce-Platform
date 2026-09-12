package com.microservices.InventoryService.controller;

import com.microservices.InventoryService.dto.InventoryRequest;
import com.microservices.InventoryService.dto.InventoryResponse;
import com.microservices.InventoryService.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> getStock(@RequestParam List<String> skuCode) {
        return inventoryService.getStock(skuCode);
    }

    @PostMapping("/check")
    public boolean checkAndReduceStock(@RequestBody List<InventoryRequest> requests){
        return inventoryService.checkAndReduceStock(requests);
    }

}