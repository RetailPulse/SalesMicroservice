package com.retailpulse.repository;

import com.retailpulse.entity.SalesTax;
import com.retailpulse.entity.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesTaxRepository extends JpaRepository<SalesTax, Long> {
    Optional<SalesTax> findSalesTaxByTaxType(TaxType taxType);
}
