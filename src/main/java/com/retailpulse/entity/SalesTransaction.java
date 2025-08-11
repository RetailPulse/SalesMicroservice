package com.retailpulse.entity;

import com.retailpulse.controller.request.SalesDetailsDto;
import com.retailpulse.util.DateUtil;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    private BigDecimal subtotal; // subtotal of items without tax

    private BigDecimal total; // total of items with tax

    @Column(nullable = false)
    @CreationTimestamp
    // Automatically set when the entity is persisted
    private Instant transactionDate;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "salesTransaction", orphanRemoval = true)
    private List<SalesDetails> salesDetailEntities = new ArrayList<>();

    protected SalesTransaction() {}

    public SalesTransaction(Long businessEntityId, SalesTax salesTax) {
        this.businessEntityId = businessEntityId;
        this.salesTax = salesTax;
    }

    public void addSalesDetails(SalesDetails detail) {
        detail.setSalesTransaction(this);
        salesDetailEntities.add(detail);
        recalculateTotal();
    }

    public void updateSalesDetails(List<SalesDetails> details) {
        this.salesDetailEntities.clear();

        for (SalesDetails detail : details) {
            this.addSalesDetails(detail);
        }
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
                this.salesDetailEntities.stream().map(
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

        for (SalesDetailsDto salesDetailsDto : memento.salesDetails()) {
            SalesDetails salesDetails = new SalesDetails(
                    salesDetailsDto.productId(),
                    salesDetailsDto.quantity(),
                    new BigDecimal(salesDetailsDto.salesPricePerUnit())
            );
            this.addSalesDetails(salesDetails);
        }

        return this;
    }

    private void recalculateTotal() {
        BigDecimal subtotal = salesDetailEntities.stream()
                .map(SalesDetails::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        this.subtotal = subtotal;
        this.salesTaxAmount = salesTax.calculateTax(subtotal);
        this.total = subtotal.add(salesTaxAmount).setScale(2, RoundingMode.HALF_UP);
    }

}
