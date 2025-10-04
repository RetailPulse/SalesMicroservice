package com.retailpulse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.retailpulse.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponseDto (
    @JsonProperty("clientSecret")
    String clientSecret,
    @JsonProperty("paymentIntentId")
    String paymentIntentId,
    @JsonProperty("paymentId")
    Long paymentId,
    @JsonProperty("transactionId")
    Long transactionId,
    @JsonProperty("totalPrice")
    Double totalPrice,
    @JsonProperty("currency")
    String currency,
    @JsonProperty("paymentStatus")
    PaymentStatus paymentStatus,
    @JsonProperty("paymentDate")
    LocalDateTime paymentEventDate
  ) {
}
