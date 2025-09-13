package com.retailpulse.service;

import com.retailpulse.client.InventoryServiceClient;
import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.dto.response.InventoryUpdateResponseDto;
import com.retailpulse.entity.SalesTransaction;
import com.retailpulse.exception.StockUpdateException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class StockUpdateService {
    
    private final InventoryServiceClient inventoryServiceClient;
    
    public StockUpdateService(InventoryServiceClient inventoryServiceClient) {
        this.inventoryServiceClient = inventoryServiceClient;
    }
    
    public InventoryUpdateResponseDto updateStocks(SalesTransaction transaction) {
        InventoryUpdateRequestDto request = createStockUpdateRequest(transaction);
        InventoryUpdateResponseDto response = inventoryServiceClient.updateStocks(request);

        if (!response.success()) {
            throw new StockUpdateException(
                    "Failed to update stock for sale transaction " + transaction.getId() + ": " + response.message()
            );
        }

        return response;
    }
        
    private InventoryUpdateRequestDto createStockUpdateRequest(SalesTransaction transaction) {
        var items = transaction.getSalesDetailEntities().values().stream()
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