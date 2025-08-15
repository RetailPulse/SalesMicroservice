package com.retailpulse.service;

import com.retailpulse.client.InventoryServiceClient;
import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.entity.SalesDetails;
import com.retailpulse.entity.SalesTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockUpdateServiceTest {

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @InjectMocks
    private StockUpdateService stockUpdateService;

    @Test
    void shouldCallDeductStockWhenDeductingStock() {
        // Given
        SalesTransaction transaction = createTestTransaction();

        // When
        stockUpdateService.deductStock(transaction);

        // Then
        verify(inventoryServiceClient, times(1)).deductStock(any(InventoryUpdateRequestDto.class));
        verify(inventoryServiceClient, never()).addStock(any());
    }

    @Test
    void shouldCallAddStockWhenAddingStock() {
        // Given
        SalesTransaction transaction = createTestTransaction();

        // When
        stockUpdateService.addStock(transaction);

        // Then
        verify(inventoryServiceClient, times(1)).addStock(any(InventoryUpdateRequestDto.class));
        verify(inventoryServiceClient, never()).deductStock(any());
    }

    @Test
    void shouldCreateCorrectInventoryUpdateRequestForDeductStock() {
        // Given
        SalesTransaction transaction = createTestTransaction();
        Long expectedBusinessEntityId = 100L;
        Long expectedProductId = 1L;
        Integer expectedQuantity = 5;

        // When
        stockUpdateService.deductStock(transaction);

        // Then
        verify(inventoryServiceClient).deductStock(argThat(request -> 
            request.businessEntityId() == expectedBusinessEntityId &&
            request.items().size() == 1 &&
            request.items().get(0).productId() == expectedProductId &&
            request.items().get(0).quantity() == expectedQuantity
        ));
    }

    @Test
    void shouldCreateCorrectInventoryUpdateRequestForAddStock() {
        // Given
        SalesTransaction transaction = createTestTransaction();
        Long expectedBusinessEntityId = 100L;
        Long expectedProductId = 1L;
        Integer expectedQuantity = 5;

        // When
        stockUpdateService.addStock(transaction);

        // Then
        verify(inventoryServiceClient).addStock(argThat(request -> 
            request.businessEntityId() == expectedBusinessEntityId &&
            request.items().size() == 1 &&
            request.items().get(0).productId() == expectedProductId &&
            request.items().get(0).quantity() == expectedQuantity
        ));
    }

    @Test
    void shouldHandleMultipleSalesDetails() {
        // Given
        SalesTransaction transaction = createTransactionWithMultipleItems();

        // When
        stockUpdateService.deductStock(transaction);

        // Then
        verify(inventoryServiceClient).deductStock(argThat(request -> 
            request.items().size() == 2 &&
            request.items().get(0).productId() == 1L &&
            request.items().get(0).quantity() == 5 &&
            request.items().get(1).productId() == 2L &&
            request.items().get(1).quantity() == 3
        ));
    }

    // Helper methods to create test data
    private SalesTransaction createTestTransaction() {
        SalesTransaction transaction = mock(SalesTransaction.class);
        when(transaction.getBusinessEntityId()).thenReturn(100L);
        
        SalesDetails salesDetails = new SalesDetails(1L, 5, new BigDecimal("10.99"));
        when(transaction.getSalesDetailEntities()).thenReturn(List.of(salesDetails));
        
        return transaction;
    }

    private SalesTransaction createTransactionWithMultipleItems() {
        SalesTransaction transaction = mock(SalesTransaction.class);
        when(transaction.getBusinessEntityId()).thenReturn(100L);
        
        SalesDetails salesDetails1 = new SalesDetails(1L, 5, new BigDecimal("10.99"));
        SalesDetails salesDetails2 = new SalesDetails(2L, 3, new BigDecimal("15.50"));
        
        when(transaction.getSalesDetailEntities()).thenReturn(List.of(salesDetails1, salesDetails2));
        
        return transaction;
    }
}