package com.retailpulse.client;

import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceClientTest {

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Test
    void shouldCallDeductStockMethod() {
        // Given
        InventoryUpdateRequestDto.InventoryItem item = 
            new InventoryUpdateRequestDto.InventoryItem(1L, 5);
        InventoryUpdateRequestDto requestDto = new InventoryUpdateRequestDto(
            100L, List.of(item)
        );

        // When
        inventoryServiceClient.deductStocks(requestDto);

        // Then
        verify(inventoryServiceClient, times(1)).deductStocks(requestDto);
        verify(inventoryServiceClient, never()).addStocks(any());
    }

    @Test
    void shouldCallAddStockMethod() {
        // Given
        InventoryUpdateRequestDto.InventoryItem item = 
            new InventoryUpdateRequestDto.InventoryItem(1L, 3);
        InventoryUpdateRequestDto requestDto = new InventoryUpdateRequestDto(
            200L, List.of(item)
        );

        // When
        inventoryServiceClient.addStocks(requestDto);

        // Then
        verify(inventoryServiceClient, times(1)).addStocks(requestDto);
        verify(inventoryServiceClient, never()).deductStocks(any());
    }
}