package com.retailpulse.controller;

import com.retailpulse.controller.request.SalesDetailsDto;
import com.retailpulse.controller.request.SalesTransactionRequestDto;
import com.retailpulse.controller.request.SuspendedTransactionDto;
import com.retailpulse.controller.response.SalesTransactionResponseDto;
import com.retailpulse.controller.response.TaxResultDto;
import com.retailpulse.controller.response.TransientSalesTransactionDto;
import com.retailpulse.service.SalesTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesTransactionController {
    private final SalesTransactionService salesTransactionService;

    @Autowired
    public SalesTransactionController(SalesTransactionService salesTransactionService) {
        this.salesTransactionService = salesTransactionService;
    }

    @PostMapping("/calculateSalesTax")
    public ResponseEntity<TaxResultDto> calculateSalesTax(@RequestBody List<SalesDetailsDto> salesDetailsDtos) {
        TaxResultDto taxResultDto = salesTransactionService.calculateSalesTax(salesDetailsDtos);
        return ResponseEntity.ok(taxResultDto);
    }

    /**
     * Endpoint to create a new SalesTransaction.
     *
     * @param requestDto the SalesTransactionRequestDto containing transaction details
     * @return the created SalesTransaction
     */
    @PostMapping("/createTransaction")
    public ResponseEntity<SalesTransactionResponseDto> createSalesTransaction(@RequestBody SalesTransactionRequestDto requestDto) {
        SalesTransactionResponseDto responseDto = salesTransactionService.createSalesTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    /**
     * Endpoint to update an existing SalesTransaction.
     *
     * @param transactionId          the ID of the SalesTransaction to update
     * @param newSalesDetailEntities a list of new SalesDetails to replace the old ones
     * @return the updated SalesTransaction
     */
    @PutMapping("updateSalesTransaction/{transactionId}")
    public ResponseEntity<SalesTransactionResponseDto> updateSalesTransaction(
            @PathVariable Long transactionId,
            @RequestBody List<SalesDetailsDto> newSalesDetailEntities) {
        SalesTransactionResponseDto updatedTransaction = salesTransactionService.updateSalesTransaction(transactionId, newSalesDetailEntities);
        return ResponseEntity.ok(updatedTransaction);
    }

    @PostMapping("/suspend")
    public ResponseEntity<List<TransientSalesTransactionDto>> suspendTransaction(@RequestBody SuspendedTransactionDto suspendedTransactionDto) {
        List<TransientSalesTransactionDto> transactionHistory = salesTransactionService.suspendTransaction(suspendedTransactionDto);
        return ResponseEntity.ok(transactionHistory);
    }

    @DeleteMapping("/{businessEntityId}/suspended-transactions/{transactionId}")
    public ResponseEntity<List<TransientSalesTransactionDto>> restoreTransaction(@PathVariable Long businessEntityId, @PathVariable Long transactionId) {
        List<TransientSalesTransactionDto> transactionHistory = salesTransactionService.restoreTransaction(businessEntityId, transactionId);
        return ResponseEntity.ok(transactionHistory);
    }

}