package com.retailpulse.service;

import com.retailpulse.controller.request.SalesDetailsDto;
import com.retailpulse.controller.request.SalesTransactionRequestDto;
import com.retailpulse.controller.request.SuspendedTransactionDto;
import com.retailpulse.controller.response.SalesTransactionResponseDto;
import com.retailpulse.controller.response.TaxResultDto;
import com.retailpulse.controller.response.TransientSalesTransactionDto;
import com.retailpulse.entity.*;
import com.retailpulse.exception.ErrorCodes;
import com.retailpulse.repository.SalesTaxRepository;
import com.retailpulse.repository.SalesTransactionRepository;
import com.retailpulse.exception.BusinessException;
import com.retailpulse.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class SalesTransactionService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesTaxRepository salesTaxRepository;
    private final SalesTransactionHistory salesTransactionHistory;
    // private final StockUpdateService stockUpdateService;

    public SalesTransactionService(SalesTransactionRepository salesTransactionRepository,
                                   SalesTaxRepository salesTaxRepository,
                                   SalesTransactionHistory salesTransactionHistory //,
                                  //  StockUpdateService stockUpdateService
                                   ) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.salesTaxRepository = salesTaxRepository;
        this.salesTransactionHistory = salesTransactionHistory;
        // this.stockUpdateService = stockUpdateService;
    }


    public TaxResultDto calculateSalesTax(List<SalesDetailsDto> salesDetailsDtos) {
        BigDecimal subtotal = salesDetailsDtos.stream()
                .map(salesDetailsDto -> new BigDecimal(salesDetailsDto.salesPricePerUnit()).multiply(new BigDecimal(salesDetailsDto.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        SalesTax salesTax = salesTaxRepository.findSalesTaxByTaxType(TaxType.GST)
                .orElseGet(() -> {
                    SalesTax newSalesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));
                    return salesTaxRepository.save(newSalesTax);
                });

        BigDecimal taxAmount = subtotal.multiply(salesTax.getTaxRate()).setScale(2, RoundingMode.HALF_UP);

        return new TaxResultDto(subtotal.toString(),
                salesTax.getTaxType().name(),
                salesTax.getTaxRate().toString(),
                taxAmount.toString(),
                subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP).toString(),
                salesDetailsDtos);
    }

    /**
     * Creates a new SalesTransaction with the provided details.
     *
     * @param requestDto the SalesTransactionRequestDto containing the details of the transaction
     * @return the created SalesTransactionResponseDto
     */
    @Transactional
    public SalesTransactionResponseDto createSalesTransaction(SalesTransactionRequestDto requestDto) {

        SalesTax salesTax = salesTaxRepository.findSalesTaxByTaxType(TaxType.GST)
                .orElseGet(() -> {
                    SalesTax newSalesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));
                    return salesTaxRepository.save(newSalesTax);
                });

        // Create a sales transaction with the provided businessEntityId and computed subtotal
        SalesTransaction transaction = new SalesTransaction(requestDto.businessEntityId(), salesTax);

        // map salesDetailsDto to salesDetails
        List<SalesDetails> salesDetailEntities = requestDto.salesDetails().stream()
                .map(salesDetailsDto -> new SalesDetails(salesDetailsDto.productId(), salesDetailsDto.quantity(), new BigDecimal(salesDetailsDto.salesPricePerUnit())))
                .toList();

        // Add each SalesDetails to the transaction
        salesDetailEntities.forEach(transaction::addSalesDetails);

        // TODO: Change to call inventory service to deduct stock
        // // For each SalesDetails entry, deduct inventory
        // stockUpdateService.deductStock(transaction);

        transaction = salesTransactionRepository.save(transaction);

        // map salesTransaction to salesTransactionResponseDto
        return mapToResponseDto(transaction);

    }

    /**
     * Updates an existing SalesTransaction with new sales details.
     *
     * @param transactionId       the ID of the SalesTransaction to update
     * @param newSalesDetailsDtos the new sales details to update
     * @return the updated SalesTransactionResponseDto
     */
    @Transactional
    public SalesTransactionResponseDto updateSalesTransaction(Long transactionId, List<SalesDetailsDto> newSalesDetailsDtos) {
        // Retrieve the existing transaction
        SalesTransaction existingTransaction = salesTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Sales transaction not found for id: " + transactionId));

        // TODO: Change to call inventory service to add inventory transaction
        // // Reverse inventory deduction for each old sales detail
        // stockUpdateService.addStock(existingTransaction);

        // Map new sales details DTOs to SalesDetails entities
        List<SalesDetails> newSalesDetailEntities = newSalesDetailsDtos.stream()
                .map(salesDetailsDto -> new SalesDetails(salesDetailsDto.productId(), salesDetailsDto.quantity(), new BigDecimal(salesDetailsDto.salesPricePerUnit())))
                .toList();

        existingTransaction.updateSalesDetails(newSalesDetailEntities);

        //TODO: Change to call inventory service to deduct stock
        // stockUpdateService.deductStock(existingTransaction);

        salesTransactionRepository.saveAndFlush(existingTransaction);

        return mapToResponseDto(existingTransaction);

    }

    /**
     * Suspends a transaction by saving its state to the history.
     *
     * @param suspendedTransactionDto the DTO containing the details of the suspended transaction
     */
    public List<TransientSalesTransactionDto> suspendTransaction(SuspendedTransactionDto suspendedTransactionDto) {
        SalesTax salesTax = salesTaxRepository.findSalesTaxByTaxType(TaxType.GST)
                .orElseGet(() -> {
                    SalesTax newSalesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));
                    return salesTaxRepository.save(newSalesTax);
                });

        SalesTransaction salesTransaction = new SalesTransaction(suspendedTransactionDto.businessEntityId(), salesTax);

        List<SalesDetails> salesDetails = suspendedTransactionDto.salesDetails().stream()
                .map(salesDetailsDto -> new SalesDetails(salesDetailsDto.productId(), salesDetailsDto.quantity(), new BigDecimal(salesDetailsDto.salesPricePerUnit())))
                .toList();

        salesDetails.forEach(salesTransaction::addSalesDetails);

        SalesTransactionMemento salesTransactionMemento = salesTransaction.saveToMemento();

        Map<Long, SalesTransactionMemento> suspendedTransactions = salesTransactionHistory.addTransaction(suspendedTransactionDto.businessEntityId(), salesTransactionMemento);

        // Map the suspended transactions to DTOs
        return suspendedTransactions.entrySet().stream()
                .map(entry -> {
                    SalesTransactionMemento memento = entry.getValue();

                    SalesTransaction transaction = new SalesTransaction(memento.businessEntityId(), salesTax);
                    transaction.restoreFromMemento(memento);

                    return mapToTransientDto(transaction);
                })
                .toList();

    }

    public List<TransientSalesTransactionDto> restoreTransaction(Long businessEntityId, Long transactionId) {
        Map<Long, SalesTransactionMemento> suspendedTransactions = salesTransactionHistory.deleteTransaction(businessEntityId, transactionId);

        // Map the suspended transactions to DTOs
        return suspendedTransactions.entrySet().stream()
                .map(entry -> {
                    SalesTransactionMemento memento = entry.getValue();

                    SalesTransaction transaction = new SalesTransaction(memento.businessEntityId(), new SalesTax(TaxType.valueOf(memento.taxType()), new BigDecimal(memento.taxRate())));
                    transaction.restoreFromMemento(memento);

                    return mapToTransientDto(transaction);
                })
                .toList();
    }

    private TransientSalesTransactionDto mapToTransientDto(SalesTransaction salesTransaction) {
        return new TransientSalesTransactionDto(
                salesTransaction.getId(),
                salesTransaction.getBusinessEntityId(),
                salesTransaction.getSubtotal().toString(),
                salesTransaction.getSalesTax().getTaxType().name(),
                salesTransaction.getSalesTax().getTaxRate().toString(),
                salesTransaction.getSalesTaxAmount().toString(),
                salesTransaction.getTotal().toString(),
                salesTransaction.getSalesDetailEntities().stream()
                        .map(salesDetails -> new SalesDetailsDto(
                                salesDetails.getProductId(),
                                salesDetails.getQuantity(),
                                salesDetails.getSalesPricePerUnit().toString()
                        ))
                        .toList(),
                DateUtil.convertInstantToString(salesTransaction.getTransactionDate(), DateUtil.DATE_TIME_FORMAT)
        );
    }

    private SalesTransactionResponseDto mapToResponseDto(SalesTransaction salesTransaction) {
        return new SalesTransactionResponseDto(
                salesTransaction.getId(),
                salesTransaction.getBusinessEntityId(),
                salesTransaction.getSubtotal().toString(),
                salesTransaction.getSalesTax().getTaxType().name(),
                salesTransaction.getSalesTax().getTaxRate().toString(),
                salesTransaction.getSalesTaxAmount().toString(),
                salesTransaction.getTotal().toString(),
                salesTransaction.getSalesDetailEntities().stream()
                        .map(salesDetails -> new SalesDetailsDto(
                                salesDetails.getProductId(),
                                salesDetails.getQuantity(),
                                salesDetails.getSalesPricePerUnit().toString()
                        ))
                        .toList(),
                DateUtil.convertInstantToString(salesTransaction.getTransactionDate(), DateUtil.DATE_TIME_FORMAT)
        );
    }

}
