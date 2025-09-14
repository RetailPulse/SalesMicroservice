package com.retailpulse.service;

import com.retailpulse.client.InventoryServiceClient;
import com.retailpulse.dto.request.InventoryUpdateRequestDto;
import com.retailpulse.entity.SalesDetails;
import com.retailpulse.exception.BusinessException;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class StockUpdateService {

  private static final Logger logger = Logger.getLogger(StockUpdateService.class.getName());

  private final InventoryServiceClient inventoryServiceClient;

  public StockUpdateService(InventoryServiceClient inventoryServiceClient) {
    this.inventoryServiceClient = inventoryServiceClient;
  }

  public void updateStocks(Long businessEntityId, Map<Long, SalesDetails> salesDetails) {
    if (salesDetails == null || salesDetails.isEmpty()) {
      logger.warning("No sales details provided for inventory update.");
      throw new BusinessException("EMPTY_TRANSACTION", "No sales details found for inventory update.");
    }

    InventoryUpdateRequestDto request = createStockUpdateRequest(businessEntityId, salesDetails);

    String productSummary = request.items().stream()
      .map(item -> "productId=" + item.productId() + ", quantity=" + item.quantity())
      .collect(Collectors.joining("; "));
    logger.info("Preparing inventory update for businessEntityId=" + businessEntityId +
      " with items: [" + productSummary + "]");

    try {
      inventoryServiceClient.updateStocks(request);
      logger.info("Inventory update successful for businessEntityId=" + businessEntityId);
    } catch (FeignException e) {
      logger.severe("Inventory update failed for businessEntityId=" + businessEntityId +
        ". Reason: " + e.getMessage());
      throw new BusinessException("INVENTORY_UPDATE_FAILED", "Failed to update inventory: " + e.getMessage());
    }
  }

  private InventoryUpdateRequestDto createStockUpdateRequest(Long businessEntityId, Map<Long, SalesDetails> salesDetails) {
    List<InventoryUpdateRequestDto.InventoryItem> items = salesDetails.values().stream()
      .map(detail -> {
        long productId = detail.getProductId();
        int quantity = detail.getQuantity();
        logger.fine("Mapping SalesDetails to InventoryItem: productId=" + productId + ", quantity=" + quantity);
        return new InventoryUpdateRequestDto.InventoryItem(productId, quantity);
      })
      .collect(Collectors.toList());

    return new InventoryUpdateRequestDto(businessEntityId, items);
  }
}
