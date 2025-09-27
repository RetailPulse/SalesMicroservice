package com.retailpulse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentResponseDto (
    @JsonProperty("clientSecret")
    String clientSecret,
    @JsonProperty("paymentIntentId")
    String paymentIntentId
  ) {
}
