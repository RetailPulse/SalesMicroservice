package com.retailpulse.entity;

import com.retailpulse.dto.request.SalesDetailsDto;
import com.retailpulse.util.DateUtil;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Entity
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessEntityId;

    @ManyToOne
    @JoinColumn(name = "sales_tax_id")
    private SalesTax salesTax;

    private BigDecimal salesTaxAmount;

    private BigDecimal subtotal;

    private BigDecimal total;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant transactionDate;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "salesTransaction", orphanRemoval = true)
    @MapKey(name = "productId")
    private Map<Long, SalesDetails> salesDetailEntities = new HashMap<>();

    protected SalesTransaction() {}

    public SalesTransaction(Long businessEntityId, SalesTax salesTax) {
        this.businessEntityId = businessEntityId;
        this.salesTax = salesTax;
    }

    public void addSalesDetails(Map<Long, SalesDetails> details) {
        details.forEach((productId, salesDetails) -> {
            salesDetails.setSalesTransaction(this);
            this.salesDetailEntities.put(productId, salesDetails);
        });
        recalculateTotal();
    }

    public void updateSalesDetails(Map<Long, SalesDetails> details) {
        this.salesDetailEntities.clear();
        this.addSalesDetails(details);
    }

    public SalesTransactionMemento saveToMemento() {
        return new SalesTransactionMemento(
                System.currentTimeMillis(),
                this.businessEntityId,
                this.subtotal.toPlainString(),
                this.salesTax.getTaxType().name(),
                this.salesTax.getTaxRate().toPlainString(),
                this.salesTaxAmount.toPlainString(),
                this.total.toPlainString(),
                this.salesDetailEntities.values().stream().map(
                        salesDetails -> new SalesDetailsDto(
                                salesDetails.getProductId(),
                                salesDetails.getQuantity(),
                                salesDetails.getSalesPricePerUnit().toString()
                        )
                ).toList(),
                DateUtil.convertInstantToString(Instant.now(), DateUtil.DATE_TIME_FORMAT)
        );
    }

    public SalesTransaction restoreFromMemento(SalesTransactionMemento memento) {
        this.id = memento.transactionId();
        this.businessEntityId = memento.businessEntityId();
        this.salesTax = new SalesTax(TaxType.valueOf(memento.taxType()), new BigDecimal(memento.taxRate()));
        this.subtotal = new BigDecimal(memento.subTotal());
        this.salesTaxAmount = new BigDecimal(memento.taxAmount());
        this.total = new BigDecimal(memento.totalAmount());
        this.transactionDate = DateUtil.convertStringToInstant(memento.transactionDateTime(), DateUtil.DATE_TIME_FORMAT);

        Map<Long, SalesDetails> restoredDetails = new HashMap<>();
        for (SalesDetailsDto dto : memento.salesDetails()) {
            SalesDetails detail = new SalesDetails(
                    dto.productId(),
                    dto.quantity(),
                    new BigDecimal(dto.salesPricePerUnit())
            );
            restoredDetails.put(dto.productId(), detail);           
        }
        this.addSalesDetails(restoredDetails);

        return this;
    }

    private void recalculateTotal() {
        BigDecimal subtotal = salesDetailEntities.values().stream()
                .map(SalesDetails::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        this.subtotal = subtotal;
        this.salesTaxAmount = salesTax.calculateTax(subtotal);
        this.total = subtotal.add(salesTaxAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
