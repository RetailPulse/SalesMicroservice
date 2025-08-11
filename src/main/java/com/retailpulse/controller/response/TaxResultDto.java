package com.retailpulse.controller.response;

import com.retailpulse.controller.request.SalesDetailsDto;

import java.util.List;

public record TaxResultDto(
        String subTotalAmount,
        String taxType,
        String taxRate,
        String taxAmount,
        String totalAmount,
        List<SalesDetailsDto> salesDetails
) {
}
