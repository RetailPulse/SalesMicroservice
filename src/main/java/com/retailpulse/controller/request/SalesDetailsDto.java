package com.retailpulse.controller.request;

public record SalesDetailsDto(
        long productId,
        int quantity,
        String salesPricePerUnit
) {
}
