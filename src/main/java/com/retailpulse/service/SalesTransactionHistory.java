package com.retailpulse.service;

import com.retailpulse.entity.SalesTransactionMemento;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SalesTransactionHistory {

    private final Map<Long, Map<Long, SalesTransactionMemento>> suspendedTransactions = new ConcurrentHashMap<>();

    public Map<Long, SalesTransactionMemento> addTransaction(Long businessEntityId, SalesTransactionMemento salesTransactionMemento) {
        if (!suspendedTransactions.containsKey(businessEntityId)) {
            Map<Long, SalesTransactionMemento> suspendedTransaction = new HashMap<>();
            suspendedTransaction.put(salesTransactionMemento.transactionId(), salesTransactionMemento);
            suspendedTransactions.put(businessEntityId, suspendedTransaction);
        } else {
            suspendedTransactions.get(businessEntityId).put(salesTransactionMemento.transactionId(), salesTransactionMemento);
        }

        return suspendedTransactions.get(businessEntityId);
    }

    public Map<Long, SalesTransactionMemento> deleteTransaction(Long businessEntityId, Long transactionId) {
        if (suspendedTransactions.containsKey(businessEntityId)) {
            Map<Long, SalesTransactionMemento> suspendedTransactionMap = suspendedTransactions.get(businessEntityId);
            suspendedTransactionMap.remove(transactionId);
        }

        return suspendedTransactions.get(businessEntityId);
    }

}
