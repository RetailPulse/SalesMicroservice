package com.retailpulse.dto.response;

public record CreateTransactionResponseDto (
    SalesTransactionResponseDto transaction,
    PaymentResponseDto paymentIntent
  ) {
}
