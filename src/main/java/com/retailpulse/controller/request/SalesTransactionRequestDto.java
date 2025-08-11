package com.retailpulse.controller.request;

import java.util.List;

public record SalesTransactionRequestDto(
        long businessEntityId,
        String taxAmount,
        String totalAmount,
        List<SalesDetailsDto> salesDetails
) {
}
