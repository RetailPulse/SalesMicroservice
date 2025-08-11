package com.retailpulse.DTO;

import com.retailpulse.entity.SalesDetails;
import com.retailpulse.entity.SalesTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * Data Transfer Object (DTO) for encapsulating a SalesTransaction and its SalesDetails.
 */
@Data
@AllArgsConstructor
public class SalesTransactionDetailsDto {
    private SalesTransaction salesTransaction;
    private List<SalesDetails> details;
    public Optional<SalesTransaction> map(Object object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'map'");
    }
}