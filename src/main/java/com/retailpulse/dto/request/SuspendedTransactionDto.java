package com.retailpulse.dto.request;

import java.util.List;

public record SuspendedTransactionDto(
        long businessEntityId,
        List<SalesDetailsDto> salesDetails
) {
}
