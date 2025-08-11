package com.retailpulse.entity;

import com.retailpulse.controller.request.SalesDetailsDto;

import java.util.List;

public record SalesTransactionMemento(
        Long transactionId,
        Long businessEntityId,
        String subTotal,
        String taxType,
        String taxRate,
        String taxAmount,
        String totalAmount,
        List<SalesDetailsDto> salesDetails,
        String transactionDateTime
) {
}
