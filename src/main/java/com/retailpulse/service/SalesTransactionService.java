package com.retailpulse.service;

import com.retailpulse.dto.request.SalesDetailsDto;
import com.retailpulse.dto.request.SalesTransactionRequestDto;
import com.retailpulse.dto.request.SuspendedTransactionDto;
import com.retailpulse.dto.request.PaymentRequestDto;
import com.retailpulse.dto.response.CreateTransactionResponseDto;
import com.retailpulse.dto.response.PaymentResponseDto;
import com.retailpulse.dto.response.SalesTransactionResponseDto;
import com.retailpulse.dto.response.TaxResultDto;
import com.retailpulse.dto.response.TransientSalesTransactionDto;
import com.retailpulse.entity.*;
import com.retailpulse.exception.ErrorCodes;
import com.retailpulse.repository.SalesTaxRepository;
import com.retailpulse.repository.SalesTransactionRepository;
import com.retailpulse.exception.BusinessException;
import com.retailpulse.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retailpulse.client.InventoryServiceClient;
import com.retailpulse.client.PaymentServiceClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class SalesTransactionService {

  private static final Logger logger = Logger.getLogger(SalesTransactionService.class.getName());

  private final SalesTransactionRepository salesTransactionRepository;
  private final SalesTaxRepository salesTaxRepository;
  private final SalesTransactionHistory salesTransactionHistory;
  private final StockUpdateService stockUpdateService;
  private final PaymentServiceClient paymentServiceClient;

  public SalesTransactionService(SalesTransactionRepository salesTransactionRepository,
                                 SalesTaxRepository salesTaxRepository,
                                 SalesTransactionHistory salesTransactionHistory,
                                 StockUpdateService stockUpdateService,
                                 PaymentServiceClient paymentServiceClient) {
    this.salesTransactionRepository = salesTransactionRepository;
    this.salesTaxRepository = salesTaxRepository;
    this.salesTransactionHistory = salesTransactionHistory;
    this.stockUpdateService = stockUpdateService;
    this.paymentServiceClient = paymentServiceClient;
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

    return new TaxResultDto(
      subtotal.toString(),
      salesTax.getTaxType().name(),
      salesTax.getTaxRate().toString(),
      taxAmount.toString(),
      subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP).toString(),
      salesDetailsDtos
    );
  }

  @Transactional
  public CreateTransactionResponseDto createSalesTransaction(SalesTransactionRequestDto requestDto) {
    if (requestDto.salesDetails() == null || requestDto.salesDetails().isEmpty()) {
      logger.warning("Attempted to create transaction with empty sales details.");
      throw new BusinessException("EMPTY_SALE", "Sales details cannot be empty.");
    }

    logger.info("Creating sales transaction for businessEntityId=" + requestDto.businessEntityId());

    SalesTax salesTax = salesTaxRepository.findSalesTaxByTaxType(TaxType.GST)
      .orElseGet(() -> {
        SalesTax newSalesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));
        return salesTaxRepository.save(newSalesTax);
      });

    SalesTransaction transaction = new SalesTransaction(requestDto.businessEntityId(), salesTax);

    Map<Long, SalesDetails> salesDetailEntities = requestDto.salesDetails().stream()
      .map(salesDetailsDto -> new SalesDetails(
        salesDetailsDto.productId(),
        salesDetailsDto.quantity(),
        new BigDecimal(salesDetailsDto.salesPricePerUnit())
      ))
      .collect(Collectors.toMap(
        SalesDetails::getProductId,
        detail -> detail,
        (_, replacement) -> replacement
      ));

    transaction.addSalesDetails(salesDetailEntities);

    try {
      stockUpdateService.updateStocks(requestDto.businessEntityId(), salesDetailEntities);
    } catch (BusinessException e) {
      logger.severe("Inventory update failed during transaction creation: " + e.getMessage());
      throw new BusinessException("INVENTORY_UPDATE_FAILED", "Inventory update failed: " + e.getMessage());
    }

    transaction = salesTransactionRepository.save(transaction);
    logger.info("Sales transaction created successfully with ID=" + transaction.getId());

    BigDecimal totalAmount = transaction.getTotal(); // Assuming getTotal() returns BigDecimal
    Long transactionId = transaction.getId();
    // Handle potential null totalAmount gracefully if needed
    if (totalAmount == null) {
      logger.severe("Transaction total amount is null for transaction ID=" + transactionId);
      // Clean up preliminary transaction if desired
      // salesTransactionRepository.deleteById(transactionId);
      throw new BusinessException("TRANSACTION_TOTAL_NULL", "Transaction total amount is null.");
    }

    PaymentRequestDto paymentData = new PaymentRequestDto(
      String.valueOf(transactionId), "RetailPulse Payment", 
      totalAmount.doubleValue(), 
      "SGD", 
      "pos@retailpulse.com", 
      "card"
    ); 
    logger.info("Prepared payment data for transaction ID=" + transactionId + ", amount=" + totalAmount);

    PaymentResponseDto paymentResponseDto;
        try {
            paymentResponseDto = paymentServiceClient.createPaymentIntent(paymentData);
            logger.info("Received payment intent for transaction ID=" + transactionId);
        } catch (Exception e) { // Catch Feign exceptions (FeignException, RetryableException, etc.)
            logger.severe("Call to Payment Microservice failed for transaction ID=" + transactionId + ": " + e.getMessage());            
            throw new BusinessException("PAYMENT_SERVICE_ERROR", "Failed to initiate payment: " + e.getMessage());
        }

    SalesTransactionResponseDto transactionResponseDto = mapToResponseDto(transaction);

    CreateTransactionResponseDto responseDto = new CreateTransactionResponseDto(transactionResponseDto, paymentResponseDto);
    logger.info("Successfully created transaction response for transaction ID=" + transactionId);

    return responseDto;
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
    if (newSalesDetailsDtos == null || newSalesDetailsDtos.isEmpty()) {
      logger.warning("Attempted to update transaction with empty sales details.");
      throw new BusinessException("EMPTY_UPDATE", "New sales details cannot be empty.");
    }

    logger.info("Updating sales transaction ID=" + transactionId + " with " + newSalesDetailsDtos.size() + " items.");

    SalesTransaction existingTransaction = salesTransactionRepository.findById(transactionId)
      .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Sales transaction not found for id: " + transactionId));

    Map<Long, SalesDetails> existingDetails = existingTransaction.getSalesDetailEntities();

    List<Long> missingProductIds = existingDetails.keySet().stream()
      .filter(productId ->
        newSalesDetailsDtos.stream()
          .map(SalesDetailsDto::productId)
          .noneMatch(id -> id.equals(productId))
      )
      .collect(Collectors.toList());

    Map<Long, SalesDetails> reversalMap = missingProductIds.stream()
      .collect(Collectors.toMap(
        productId -> productId,
        productId -> {
          SalesDetails detail = existingDetails.get(productId);
          return new SalesDetails(productId, -detail.getQuantity(), detail.getSalesPricePerUnit());
        }
      ));

    Map<Long, SalesDetails> updateSalesDetailsMap = newSalesDetailsDtos.stream()
      .collect(Collectors.toMap(
        SalesDetailsDto::productId,
        sdDTO -> {
          long productId = sdDTO.productId();
          int newQuantity = sdDTO.quantity();
          BigDecimal newPrice = new BigDecimal(sdDTO.salesPricePerUnit());

          if (existingDetails.containsKey(productId)) {
            SalesDetails existingDetail = existingDetails.get(productId);
            int deltaQuantity = newQuantity - existingDetail.getQuantity();
            return new SalesDetails(productId, deltaQuantity, newPrice);
          } else {
            return new SalesDetails(productId, newQuantity, newPrice);
          }
        }
      ));

    updateSalesDetailsMap.putAll(reversalMap);

    Map<Long, SalesDetails> newDetailsMap = newSalesDetailsDtos.stream()
      .collect(Collectors.toMap(
        SalesDetailsDto::productId,
        sdDTO -> new SalesDetails(
          sdDTO.productId(),
          sdDTO.quantity(),
          new BigDecimal(sdDTO.salesPricePerUnit())
        )
      ));

    existingTransaction.updateSalesDetails(newDetailsMap);

    try {
      stockUpdateService.updateStocks(existingTransaction.getBusinessEntityId(), updateSalesDetailsMap);
    } catch (BusinessException e) {
      logger.severe("Inventory update failed during transaction update: " + e.getMessage());
      throw new BusinessException("INVENTORY_UPDATE_FAILED", "Inventory update failed: " + e.getMessage());
    }

    salesTransactionRepository.saveAndFlush(existingTransaction);
    logger.info("Sales transaction ID=" + transactionId + " updated successfully.");

    return mapToResponseDto(existingTransaction);
  }

  public List<TransientSalesTransactionDto> suspendTransaction(SuspendedTransactionDto suspendedTransactionDto) {
    if (suspendedTransactionDto.salesDetails() == null || suspendedTransactionDto.salesDetails().isEmpty()) {
      logger.warning("Attempted to suspend transaction with empty sales details.");
      throw new BusinessException("EMPTY_SALE", "Sales details cannot be empty.");
    }

    logger.info("Suspending transaction for businessEntityId=" + suspendedTransactionDto.businessEntityId());

    SalesTax salesTax = salesTaxRepository.findSalesTaxByTaxType(TaxType.GST)
      .orElseGet(() -> {
        SalesTax newSalesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));
        return salesTaxRepository.save(newSalesTax);
      });

    SalesTransaction salesTransaction = new SalesTransaction(suspendedTransactionDto.businessEntityId(), salesTax);

    Map<Long, SalesDetails> salesDetails = suspendedTransactionDto.salesDetails().stream()
      .map(salesDetailsDto -> new SalesDetails(
        salesDetailsDto.productId(),
        salesDetailsDto.quantity(),
        new BigDecimal(salesDetailsDto.salesPricePerUnit())
      ))
      .collect(Collectors.toMap(
        SalesDetails::getProductId,
        detail -> detail,
        (_, replacement) -> replacement
      ));

    salesTransaction.addSalesDetails(salesDetails);

    SalesTransactionMemento salesTransactionMemento = salesTransaction.saveToMemento();

    Map<Long, SalesTransactionMemento> suspendedTransactions =
      salesTransactionHistory.addTransaction(suspendedTransactionDto.businessEntityId(), salesTransactionMemento);

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
    logger.info("Restoring suspended transactionId=" + transactionId + " for businessEntityId=" + businessEntityId);

    Map<Long, SalesTransactionMemento> suspendedTransactions =
      salesTransactionHistory.deleteTransaction(businessEntityId, transactionId);

    return suspendedTransactions.entrySet().stream()
      .map(entry -> {
        SalesTransactionMemento memento = entry.getValue();
        SalesTransaction transaction = new SalesTransaction(
          memento.businessEntityId(),
          new SalesTax(TaxType.valueOf(memento.taxType()), new BigDecimal(memento.taxRate()))
        );
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
      salesTransaction.getSalesDetailEntities().values().stream()
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
      salesTransaction.getSalesDetailEntities().values().stream()
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
