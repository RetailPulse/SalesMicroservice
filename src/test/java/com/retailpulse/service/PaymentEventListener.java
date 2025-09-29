// package com.retailpulse.service;

// import com.retailpulse.dto.PaymentEventDto;
// import com.retailpulse.entity.PaymentStatus;
// import com.retailpulse.entity.TransactionStatus;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDateTime;

// import static org.mockito.Mockito.*;
// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

// @ExtendWith(MockitoExtension.class)
// class PaymentEventListenerTest {

//     @Mock
//     private SalesTransactionService salesTransactionService;

//     // While we can mock the logger, asserting specific log messages is often skipped in basic unit tests
//     // unless critical. The primary focus is on verifying interactions with the mocked service.
//     // If you need to verify log calls, you'd typically use a mocking framework feature or a test logger.
//     // For this example, we'll rely on the logic paths and service interactions.
//     // private Logger logger; // Not directly mocked here for simplicity

//     @InjectMocks // This injects the mocked SalesTransactionService into the PaymentEventListener
//     private PaymentEventListener paymentEventListener; // The listener under test

//     private PaymentEventDto validPaymentEvent;
//     private PaymentEventDto unknownStatusPaymentEvent;

//     @BeforeEach
//     void setUp() {
//         // Setup standard test data objects
//         validPaymentEvent = new PaymentEventDto(
//                 10L, // paymentId
//                 "pi_123", // paymentIntentId
//                 1L, // transactionId (Long)
//                 1000L, // totalPrice
//                 "SGD", // currency
//                 "customer@example.com", // customerEmail
//                 PaymentStatus.SUCCEEDED, // paymentStatus
//                 LocalDateTime.now() // paymentDate
//         );

//         unknownStatusPaymentEvent = new PaymentEventDto(
//                 12L, "pi_125", 2L, 1000L, "SGD", "customer3@example.com", PaymentStatus.PROCESSING, LocalDateTime.now() // PROCESSING might map to null or PENDING_PAYMENT
//         );
//         // Note: PROCESSING maps to PENDING_PAYMENT based on your logic, so it's not 'unknown'.
//         // Let's create a truly unknown/unmapped status scenario if PROCESSING is handled.
//         // For now, we'll test the PROCESSING path which maps to PENDING_PAYMENT.
//         // If you had an enum value in PaymentStatus not covered by the switch, *that* would be unmapped.
//         // Let's assume PROCESSING is valid for now. We can test the service call for it.
//     }

//     @Test
//     void handlePaymentEvent_ValidMessage_StatusUpdated() {
//         // Arrange
//         // The mapPaymentStatusToTransactionStatus logic maps SUCCEEDED -> COMPLETED

//         // Act
//         paymentEventListener.handlePaymentEvent(validPaymentEvent);

//         // Assert
//         // Verify that the service method was called with the correct arguments
//         verify(salesTransactionService, times(1)).updateTransactionStatus(eq(1L), eq(TransactionStatus.COMPLETED), any());
//         // Verify service is NOT called for other invalid scenarios in this test
//         verify(salesTransactionService, never()).updateTransactionStatus(anyLong(), eq(TransactionStatus.PENDING_PAYMENT), any()); // Not called in this scenario
//     }

//     @Test
//     void handlePaymentEvent_NullTransactionId_DoesNotCallService() {
//         // Arrange
//         PaymentEventDto eventWithNullTxId = new PaymentEventDto(
//                 13L, "pi_126", null, 1000L, "SGD", "customer4@example.com", PaymentStatus.SUCCEEDED, LocalDateTime.now()
//         );

//         // Act
//         paymentEventListener.handlePaymentEvent(eventWithNullTxId);

//         // Assert
//         // Verify service is NOT called for null transaction ID
//         verify(salesTransactionService, never()).updateTransactionStatus(anyLong(), any(TransactionStatus.class), any());
//     }

//     @Test
//     void handlePaymentEvent_EmptyTransactionId_DoesNotCallService() {
//         // Arrange
//         PaymentEventDto eventWithEmptyTxId = new PaymentEventDto(
//                 14L, "pi_127", null, 1000L, "SGD", "customer5@example.com", PaymentStatus.SUCCEEDED, LocalDateTime.now()
//         );

//         // Act
//         paymentEventListener.handlePaymentEvent(eventWithEmptyTxId);

//         // Assert
//         // Verify service is NOT called for empty transaction ID
//         verify(salesTransactionService, never()).updateTransactionStatus(anyLong(), any(TransactionStatus.class), any());
//     }

//     // Test case for PROCESSING status (maps to PENDING_PAYMENT)
//     @Test
//     void handlePaymentEvent_ProcessingStatus_CallsServiceWithPendingPayment() {
//          // Arrange
//          PaymentEventDto processingEvent = new PaymentEventDto(
//                 15L, "pi_128", 3L, 500L, "SGD", "customer6@example.com", PaymentStatus.PROCESSING, LocalDateTime.now()
//          );

//          // Act
//          paymentEventListener.handlePaymentEvent(processingEvent);

//          // Assert
//          verify(salesTransactionService, times(1)).updateTransactionStatus(eq(3L), eq(TransactionStatus.PENDING_PAYMENT), any());
//          verify(salesTransactionService, never()).updateTransactionStatus(anyLong(), eq(TransactionStatus.COMPLETED), any());
//     }


//     @Test
//     void handlePaymentEvent_ServiceThrowsException_HandlesGracefully() throws Exception { // Assuming updateTransactionStatus declares throws
//         // Arrange
//         // Simulate the service throwing an exception when called
//         doThrow(new RuntimeException("Simulated service error"))
//                 .when(salesTransactionService).updateTransactionStatus(eq(1L), any(TransactionStatus.class), any());

//         // Act & Assert
//         // The listener method should catch the exception and not propagate it
//         // assertDoesNotThrow is useful here to confirm the listener itself doesn't crash
//         assertDoesNotThrow(() -> {
//             paymentEventListener.handlePaymentEvent(validPaymentEvent);
//         });

//         // Assert that the service method was indeed called (to trigger the mock exception)
//         verify(salesTransactionService, times(1)).updateTransactionStatus(eq(1L), eq(TransactionStatus.COMPLETED), any());
//     }
// }