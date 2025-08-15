package com.retailpulse.service;

import com.retailpulse.client.InventoryServiceClient;
import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.entity.SalesTransaction;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class StockUpdateService {
    
    private final InventoryServiceClient inventoryServiceClient;
    
    public StockUpdateService(InventoryServiceClient inventoryServiceClient) {
        this.inventoryServiceClient = inventoryServiceClient;
    }
    
    public void deductStock(SalesTransaction transaction) {
        InventoryUpdateRequestDto request = createStockUpdateRequest(transaction);
        inventoryServiceClient.deductStock(request);
    }
    
    public void addStock(SalesTransaction transaction) {
        InventoryUpdateRequestDto request = createStockUpdateRequest(transaction);
        inventoryServiceClient.addStock(request);
    }
    
    private InventoryUpdateRequestDto createStockUpdateRequest(SalesTransaction transaction) {
        var items = transaction.getSalesDetailEntities().stream()
            .map(detail -> new InventoryUpdateRequestDto.InventoryItem(
                detail.getProductId(),
                detail.getQuantity()
            ))
            .collect(Collectors.toList());
            
        return new InventoryUpdateRequestDto(
            transaction.getBusinessEntityId(),
            items
        );
    }
}