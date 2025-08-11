package com.retailpulse.controller.response;

import com.retailpulse.controller.request.SalesDetailsDto;

import java.util.List;

public record TransientSalesTransactionDto(
        long transactionId,
        long businessEntityId,
        String subTotalAmount,
        String taxType,
        String taxRate,
        String taxAmount,
        String totalAmount,
        List<SalesDetailsDto> salesDetails,
        String transactionDateTime
) {
}
