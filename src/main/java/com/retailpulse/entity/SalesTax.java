package com.retailpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Entity
public class SalesTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TaxType taxType;

    private BigDecimal taxRate;

    protected SalesTax() {
    }

    public SalesTax(TaxType taxType, BigDecimal taxRate) {
        this.taxType = taxType;
        this.taxRate = taxRate;
    }

    public BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(this.taxRate).setScale(2, RoundingMode.HALF_UP);
    }

}
