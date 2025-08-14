package com.retailpulse.dto.request;

public record SalesDetailsDto(
        long productId,
        int quantity,
        String salesPricePerUnit
) {
}
