package com.retailpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Entity
public class SalesDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    private SalesTransaction salesTransaction;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal salesPricePerUnit;

    protected SalesDetails() {}

    public SalesDetails(Long productId, int quantity, BigDecimal salesPricePerUnit) {
        this.productId = productId;
        this.quantity = quantity;
        this.salesPricePerUnit = salesPricePerUnit;
    }

    public BigDecimal getSubTotal() {
        return salesPricePerUnit.multiply(BigDecimal.valueOf(quantity));
    }
}
