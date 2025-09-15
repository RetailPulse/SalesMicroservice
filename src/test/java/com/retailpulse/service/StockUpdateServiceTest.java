package com.retailpulse.service;

import com.retailpulse.client.InventoryServiceClient;
import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.entity.SalesDetails;
import com.retailpulse.exception.BusinessException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockUpdateServiceTest {

  private InventoryServiceClient inventoryServiceClient;
  private StockUpdateService stockUpdateService;

  @BeforeEach
  void setUp() {
    inventoryServiceClient = mock(InventoryServiceClient.class);
    stockUpdateService = new StockUpdateService(inventoryServiceClient);
  }

  @Test
  void updateStocks_successfulUpdate_callsInventoryClient() {
    Long businessEntityId = 1L;
    Map<Long, SalesDetails> salesDetails = Map.of(
      100L, new SalesDetails(100L, 5, new BigDecimal("10.00")),
      101L, new SalesDetails(101L, -2, new BigDecimal("12.50"))
    );

    assertDoesNotThrow(() -> stockUpdateService.updateStocks(businessEntityId, salesDetails));
    verify(inventoryServiceClient, times(1)).updateStocks(any(InventoryUpdateRequestDto.class));
  }

  @Test
  void updateStocks_emptySalesDetails_throwsBusinessException() {
    Long businessEntityId = 1L;
    Map<Long, SalesDetails> emptyDetails = Map.of();

    BusinessException ex = assertThrows(BusinessException.class, () ->
      stockUpdateService.updateStocks(businessEntityId, emptyDetails)
    );

    assertEquals("EMPTY_TRANSACTION", ex.getErrorCode());
  }

  @Test
  void updateStocks_feignException_throwsBusinessException() {
    Long businessEntityId = 1L;
    Map<Long, SalesDetails> salesDetails = Map.of(
      100L, new SalesDetails(100L, 3, new BigDecimal("9.99"))
    );

    doThrow(mock(FeignException.class)).when(inventoryServiceClient).updateStocks(any());

    BusinessException ex = assertThrows(BusinessException.class, () ->
      stockUpdateService.updateStocks(businessEntityId, salesDetails)
    );

    assertEquals("INVENTORY_UPDATE_FAILED", ex.getErrorCode());
  }
}
