package com.retailpulse.dto.request;

import java.util.List;

public record InventoryUpdateRequestDto(
    long businessEntityId,
    List<InventoryItem> items
) {
    public record InventoryItem(
        long productId,
        int quantity
    ) {}
}
