package com.retailpulse.dto.response;

import java.util.List;

public record InventoryUpdateResponseDto(
    boolean success,
    List<ItemStatus> itemStatuses,
    String message // Optional summary or global error
) {
    public record ItemStatus(
        long productId,
        boolean updated,
        String reason // e.g. "Insufficient stock", "Invalid productId", "OK"
    ) {}
}
