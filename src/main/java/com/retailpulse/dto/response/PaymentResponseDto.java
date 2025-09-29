package com.retailpulse.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PaymentResponseDto (
    @JsonProperty("clientSecret")
    String clientSecret,
    @JsonProperty("paymentId")
    Long paymentId,
    @JsonProperty("paymentIntentId")
    String paymentIntentId,
    @JsonProperty("paymentEventDate")
    LocalDateTime paymentEventDate
  ) {
}
