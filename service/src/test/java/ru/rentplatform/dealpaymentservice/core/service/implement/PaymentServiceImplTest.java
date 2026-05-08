package ru.rentplatform.dealpaymentservice.core.service.implement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.PaymentConfirmationResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.api.exception.InvalidDealStatusException;
import ru.rentplatform.dealpaymentservice.config.YooKassaProperties;
import ru.rentplatform.dealpaymentservice.core.dao.entity.*;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealConfirmationRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.TransactionRepository;
import ru.rentplatform.dealpaymentservice.core.util.DealResponseBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DealConfirmationRepository dealConfirmationRepository;

    @Mock
    private YooKassaProperties properties;

    @Mock
    private DealResponseBuilder dealResponseBuilder;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID dealId;

    private UUID ownerId;

    private UUID renterId;

    private Deal deal;

    @BeforeEach
    void setUp() {

        dealId = UUID.randomUUID();

        ownerId = UUID.randomUUID();

        renterId = UUID.randomUUID();

        deal = Deal.builder()
                .id(dealId).itemId(UUID.randomUUID())
                .ownerId(ownerId).renterId(renterId)
                .startDate(OffsetDateTime.now().plusDays(1))
                .endDate(OffsetDateTime.now().plusDays(3))
                .pricingMode(PricingMode.DAY)
                .totalPrice(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("500"))
                .status(DealStatus.CONFIRMED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        lenient().doNothing().when(dealResponseBuilder).saveStatusHistory(
                any(), any(), any(), any(), any(), any());
        lenient().when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(dealResponseBuilder.buildDealResponse(any(Deal.class)))
                .thenReturn(DealResponse.builder().id(dealId).status(deal.getStatus().name()).build());
    }

    @Test
    void createPayment_shouldCreateMockPayment_whenMockEnabled() {

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(properties.isMockEnabled()).thenReturn(true);

        PaymentConfirmationResponse result = paymentService.createPayment(dealId, ownerId);

        assertNotNull(result);
        assertEquals("pending", result.getStatus());
        assertTrue(result.getPaymentId().startsWith("mock_payment_"));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void createPayment_shouldThrow_whenNotOwner() {

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        assertThrows(DealAccessDeniedException.class, () ->
                paymentService.createPayment(dealId, renterId));
    }

    @Test
    void createPayment_shouldThrow_whenNotConfirmed() {

        deal.setStatus(DealStatus.PENDING);
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        assertThrows(InvalidDealStatusException.class, () ->
                paymentService.createPayment(dealId, ownerId));
    }

    @Test
    void handlePaymentSuccess_shouldUpdateStatus_whenPaymentPending() {

        String paymentId = "payment_123";
        deal.setStatus(DealStatus.PAYMENT_PENDING);

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID()).deal(deal)
                .type(TransactionType.RENTAL).amount(new BigDecimal("1000"))
                .status(TransactionStatus.PENDING).yookassaPaymentId(paymentId)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();

        when(transactionRepository.findAll()).thenReturn(List.of(transaction));

        DealResponse result = paymentService.handlePaymentSuccess(paymentId);

        assertNotNull(result);
        verify(dealResponseBuilder).buildDealResponse(deal);
    }

    @Test
    void handlePaymentSuccess_shouldThrow_whenTransactionNotFound() {

        when(transactionRepository.findAll()).thenReturn(List.of());

        assertThrows(DealNotFoundException.class, () ->
                paymentService.handlePaymentSuccess("nonexistent"));
    }

    @Test
    void confirmStartDeal_shouldStart_whenBothConfirm() {

        deal.setStatus(DealStatus.PAYMENT_PENDING);

        DealConfirmation confirmation = DealConfirmation.builder()
                .id(UUID.randomUUID()).deal(deal).userId(ownerId)
                .action("START").confirmedAt(OffsetDateTime.now()).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealConfirmationRepository.existsByDeal_IdAndUserIdAndAction(dealId, ownerId, "START"))
                .thenReturn(false);
        when(dealConfirmationRepository.save(any(DealConfirmation.class))).thenReturn(confirmation);
        when(dealConfirmationRepository.countByDeal_IdAndAction(dealId, "START")).thenReturn(2L);

        DealResponse result = paymentService.confirmStartDeal(dealId, ownerId);

        assertNotNull(result);
        assertEquals(DealStatus.ACTIVE, deal.getStatus());
    }

    @Test
    void confirmStartDeal_shouldThrow_whenNotParticipant() {

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        assertThrows(DealAccessDeniedException.class, () ->
                paymentService.confirmStartDeal(dealId, UUID.randomUUID()));
    }

    @Test
    void confirmCompleteDeal_shouldComplete_whenBothConfirm_itemOk() {

        deal.setStatus(DealStatus.ACTIVE);

        Transaction rentalTx = Transaction.builder()
                .id(UUID.randomUUID()).deal(deal)
                .type(TransactionType.RENTAL).amount(new BigDecimal("1000"))
                .status(TransactionStatus.HELD)
                .yookassaPaymentId("payment_123")
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();

        DealConfirmation confirmation = DealConfirmation.builder()
                .id(UUID.randomUUID()).deal(deal).userId(ownerId)
                .action("COMPLETE").confirmedAt(OffsetDateTime.now()).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealConfirmationRepository.existsByDeal_IdAndUserIdAndAction(dealId, ownerId, "COMPLETE"))
                .thenReturn(false);
        when(dealConfirmationRepository.save(any(DealConfirmation.class))).thenReturn(confirmation);
        when(dealConfirmationRepository.countByDeal_IdAndAction(dealId, "COMPLETE")).thenReturn(2L);
        when(transactionRepository.findAllByDeal_Id(dealId)).thenReturn(List.of(rentalTx));
        when(properties.isMockEnabled()).thenReturn(true);

        DealResponse result = paymentService.confirmCompleteDeal(dealId, ownerId, true);

        assertNotNull(result);
        assertEquals(DealStatus.COMPLETED, deal.getStatus());
    }
}
