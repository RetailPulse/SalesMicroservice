package com.retailpulse.dto.response;

import com.retailpulse.entity.TransactionStatus;

public record TransactionStatusResponseDto(
    Long transactionId,
    TransactionStatus status
) {}