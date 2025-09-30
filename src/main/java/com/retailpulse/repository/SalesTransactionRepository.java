package com.retailpulse.repository;

import com.retailpulse.entity.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {
    Optional<SalesTransaction> findByPaymentId(Long paymentId);
}
