package com.retailpulse.service;

import com.retailpulse.dto.request.SalesDetailsDto;
import com.retailpulse.dto.request.SalesTransactionRequestDto;
import com.retailpulse.dto.response.SalesTransactionResponseDto;
import com.retailpulse.dto.response.TaxResultDto;
import com.retailpulse.entity.SalesDetails;
import com.retailpulse.entity.SalesTax;
import com.retailpulse.entity.SalesTransaction;
import com.retailpulse.entity.TaxType;
import com.retailpulse.repository.SalesTaxRepository;
import com.retailpulse.repository.SalesTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesTransactionServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private SalesTaxRepository salesTaxRepository;

    @Mock
    private StockUpdateService stockUpdateService;

    @InjectMocks
    private SalesTransactionService salesTransactionService;

    SalesTransactionRequestDto salesTransactionRequestDto;
    List<SalesDetailsDto> salesDetailsDtos;
    SalesTransaction dummySalesTransaction;
    SalesTax dummySalesTax;

    @BeforeEach
    public void setUp() {
        SalesDetailsDto salesDetailsDto1 = new SalesDetailsDto(1L, 2, "50.0");
        SalesDetailsDto salesDetailsDto2 = new SalesDetailsDto(2L, 3, "100.0");
        SalesDetailsDto salesDetailsDto3 = new SalesDetailsDto(3L, 4, "200.0");
        salesDetailsDtos = List.of(salesDetailsDto1, salesDetailsDto2, salesDetailsDto3);
        salesTransactionRequestDto = new SalesTransactionRequestDto(1L, "108.00", "1308.00", salesDetailsDtos);

        dummySalesTax = new SalesTax(TaxType.GST, new BigDecimal("0.09"));

        List<SalesDetails> salesDetails = salesDetailsDtos.stream().map(salesDetailsDto -> new SalesDetails(salesDetailsDto.productId(), salesDetailsDto.quantity(), new BigDecimal(salesDetailsDto.salesPricePerUnit()))).toList();
        dummySalesTransaction = new SalesTransaction(1L, dummySalesTax);
        salesDetails.forEach(dummySalesTransaction::addSalesDetails);
    }

    @Test
    public void testCreateSalesTransaction() {
        when(salesTaxRepository.save(any(SalesTax.class))).thenReturn(dummySalesTax);
        // Mock repository save() method using thenAnswer()
        when(salesTransactionRepository.save(any(SalesTransaction.class))).thenAnswer(invocation -> {
            SalesTransaction arg = invocation.getArgument(0);
            setPrivateField(arg, "id", 1L);   // Simulate auto-generated ID
            setPrivateField(arg, "transactionDate", Instant.now());
            return arg;
        });

        SalesTransactionResponseDto responseDto = salesTransactionService.createSalesTransaction(salesTransactionRequestDto);

        verify(stockUpdateService, times(1)).deductStock(any(SalesTransaction.class));
        
        verify(salesTransactionRepository, times(1)).save(any(SalesTransaction.class));
        assertEquals(TaxType.GST.name(), responseDto.taxType());
        assertEquals(salesTransactionRequestDto.totalAmount(), responseDto.totalAmount());
        assertEquals(salesTransactionRequestDto.taxAmount(), responseDto.taxAmount());
    }

    @Test
    public void testUpdateSalesTransaction() {
        SalesDetailsDto newDetail = new SalesDetailsDto(1L, 5, "50.0");
        List<SalesDetailsDto> newDetails = Collections.singletonList(newDetail);

        setPrivateField(dummySalesTransaction, "id", 1L);
        setPrivateField(dummySalesTransaction, "transactionDate", Instant.now());
        when(salesTransactionRepository.findById(1L)).thenReturn(Optional.of(dummySalesTransaction));

        SalesTransactionResponseDto salesTransactionResponseDto = salesTransactionService.updateSalesTransaction(1L, newDetails);

        verify(stockUpdateService, times(1)).addStock(any(SalesTransaction.class));
        verify(stockUpdateService, times(1)).deductStock(any(SalesTransaction.class));

        // Verify that the sales details were saved
        verify(salesTransactionRepository, times(1)).saveAndFlush(any(SalesTransaction.class));

        assertEquals(1, salesTransactionResponseDto.salesDetails().size());
        assertEquals("250.00", salesTransactionResponseDto.subTotalAmount());
        assertEquals("22.50", salesTransactionResponseDto.taxAmount());
        assertEquals("272.50", salesTransactionResponseDto.totalAmount());

    }

    @Test
    public void testCalculateSalesTax() {
        when(salesTaxRepository.save(any(SalesTax.class))).thenReturn(dummySalesTax);

        TaxResultDto taxResultDto = salesTransactionService.calculateSalesTax(salesDetailsDtos);

        assertEquals("GST", taxResultDto.taxType());
        assertEquals("0.09", taxResultDto.taxRate());
        assertEquals("1200.00", taxResultDto.subTotalAmount());
        assertEquals("108.00", taxResultDto.taxAmount());
        assertEquals("1308.00", taxResultDto.totalAmount());
    }

    private <T, V> void setPrivateField(T targetObject, String fieldName, V value) {
        try {
            Field field = targetObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // Allow access to private field
            field.set(targetObject, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on " + targetObject.getClass().getSimpleName(), e);
        }
    }

}
