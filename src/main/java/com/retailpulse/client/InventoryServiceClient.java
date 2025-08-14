// Create this interface in a new package: com.retailpulse.client
package com.retailpulse.client;

import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "${inventory-service.url}")
public interface InventoryServiceClient {
    
    @PostMapping("/api/inventory/deduct")
    void deductStock(@RequestBody InventoryUpdateRequestDto request);
    
    @PostMapping("/api/inventory/add")
    void addStock(@RequestBody InventoryUpdateRequestDto request);
}