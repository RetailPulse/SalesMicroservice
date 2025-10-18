package com.retailpulse.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentRequestDto (
    @JsonProperty("transaction_id") 
    Long transactionId,
    @JsonProperty("description")
    String description,
    @JsonProperty("amount")
    Double totalPrice,
    @JsonProperty("currency")
    String currency,
    @JsonProperty("customer_email")
    String customerEmail,
    @JsonProperty("payment_type")
    String paymentType
  ) {
}
