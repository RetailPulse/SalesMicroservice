package com.retailpulse.domain;

import com.retailpulse.entity.SalesDetails;
import com.retailpulse.entity.SalesTax;
import com.retailpulse.entity.SalesTransaction;
import com.retailpulse.entity.TaxType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SalesTransactionTest {

    @Test
    public void createSalesTransaction() {
        SalesTax salesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));
        SalesTransaction salesTransaction = new SalesTransaction(1L, salesTax);
        salesTransaction.addSalesDetails(new SalesDetails(1L, 2, new BigDecimal("50.0")));
        salesTransaction.addSalesDetails(new SalesDetails(2L, 3, new BigDecimal("100.0")));
        salesTransaction.addSalesDetails(new SalesDetails(3L, 4, new BigDecimal("200.0")));

        // expected value
        BigDecimal expectedSubtotal = new BigDecimal("50.0").multiply(new BigDecimal("2"))
                .add(new BigDecimal("100.0").multiply(new BigDecimal("3")))
                .add(new BigDecimal("200.0").multiply(new BigDecimal("4")));
        BigDecimal expectedSalesTaxAmount = expectedSubtotal.multiply(new BigDecimal("0.09"));
        BigDecimal expectedTotal = expectedSubtotal.add(expectedSalesTaxAmount);

        // assertions
        assertThat(salesTransaction.getSubtotal()).isEqualTo(expectedSubtotal.setScale(2, RoundingMode.HALF_UP));
        assertThat(salesTransaction.getSalesTaxAmount()).isEqualTo(expectedSalesTaxAmount.setScale(2, RoundingMode.HALF_UP));
        assertThat(salesTransaction.getTotal()).isEqualTo(expectedTotal.setScale(2, RoundingMode.HALF_UP));

    }

}
