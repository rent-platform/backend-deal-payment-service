package ru.rentplatform.dealpaymentservice.core.service.implement;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.PaymentConfirmationResponse;
import ru.rentplatform.dealpaymentservice.api.exception.*;
import ru.rentplatform.dealpaymentservice.client.YooKassaClient;
import ru.rentplatform.dealpaymentservice.client.audit.AuditClient;
import ru.rentplatform.dealpaymentservice.client.yookassa.dto.*;
import ru.rentplatform.dealpaymentservice.config.YooKassaProperties;
import ru.rentplatform.dealpaymentservice.core.dao.entity.*;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealConfirmationRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.TransactionRepository;
import ru.rentplatform.dealpaymentservice.core.service.PaymentService;
import ru.rentplatform.dealpaymentservice.core.util.DealResponseBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final DealRepository dealRepository;
    private final TransactionRepository transactionRepository;
    private final DealConfirmationRepository dealConfirmationRepository;
    private final YooKassaClient yooKassaClient;
    private final YooKassaProperties properties;
    private final DealResponseBuilder dealResponseBuilder;
    private final AuditClient auditClient;

    @Override
    @Transactional
    public PaymentConfirmationResponse createPayment(UUID dealId, UUID ownerId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getOwnerId().equals(ownerId)) {
            throw new DealAccessDeniedException("Only owner can create payment");
        }

        if (deal.getStatus() != DealStatus.CONFIRMED) {
            throw new InvalidDealStatusException("Deal must be in CONFIRMED status to create payment");
        }

        if (properties.isMockEnabled()) {
            String mockPaymentId = "mock_payment_" + dealId + "_" + System.currentTimeMillis();

            Transaction rentalTransaction = Transaction.builder()
                    .deal(deal)
                    .type(TransactionType.RENTAL)
                    .amount(deal.getTotalPrice())
                    .status(TransactionStatus.PENDING)
                    .yookassaPaymentId(mockPaymentId)
                    .gatewayResponse(Map.of("mock", true))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            transactionRepository.save(rentalTransaction);

            if (deal.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
                Transaction depositTransaction = Transaction.builder()
                        .deal(deal)
                        .type(TransactionType.DEPOSIT_HOLD)
                        .amount(deal.getDepositAmount())
                        .status(TransactionStatus.PENDING)
                        .yookassaPaymentId(mockPaymentId)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                transactionRepository.save(depositTransaction);
            }

            DealStatus oldStatus = deal.getStatus();
            deal.setStatus(DealStatus.PAYMENT_PENDING);
            deal.setUpdatedAt(OffsetDateTime.now());

            dealResponseBuilder.saveStatusHistory(
                    deal, oldStatus, DealStatus.PAYMENT_PENDING, ownerId,
                    DealChangeSource.USER, "Payment link created (MOCK)"
            );

            return PaymentConfirmationResponse.builder()
                    .paymentId(mockPaymentId)
                    .confirmationUrl("http://localhost:3000/payment/mock/" + mockPaymentId)
                    .status("pending")
                    .build();
        }

        BigDecimal totalAmount = deal.getTotalPrice().add(deal.getDepositAmount());

        String idempotenceKey = "payment_" + dealId;

        YooKassaPaymentRequest request = YooKassaPaymentRequest.builder()
                .amount(YooKassaPaymentRequest.Amount.builder()
                        .value(totalAmount.setScale(2, RoundingMode.HALF_UP).toString())
                        .currency("RUB")
                        .build())
                .paymentMethodData(YooKassaPaymentRequest.PaymentMethodData.builder()
                        .type("bank_card")
                        .build())
                .confirmation(YooKassaPaymentRequest.Confirmation.builder()
                        .type("redirect")
                        .returnUrl(properties.getReturnUrl())
                        .build())
                .description("Rent payment for deal " + dealId)
                .capture(false)
                .metadata(Map.of("deal_id", dealId.toString()))
                .build();

        YooKassaPaymentResponse response = yooKassaClient.createPayment(request, idempotenceKey);

        Transaction rentalTransaction = Transaction.builder()
                .deal(deal)
                .type(TransactionType.RENTAL)
                .amount(deal.getTotalPrice())
                .status(TransactionStatus.PENDING)
                .yookassaPaymentId(response.getId())
                .gatewayResponse(objectToMap(response))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        transactionRepository.save(rentalTransaction);

        if (deal.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            Transaction depositTransaction = Transaction.builder()
                    .deal(deal)
                    .type(TransactionType.DEPOSIT_HOLD)
                    .amount(deal.getDepositAmount())
                    .status(TransactionStatus.PENDING)
                    .yookassaPaymentId(response.getId())
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            transactionRepository.save(depositTransaction);
        }

        DealStatus oldStatus = deal.getStatus();
        deal.setStatus(DealStatus.PAYMENT_PENDING);
        deal.setUpdatedAt(OffsetDateTime.now());

        dealResponseBuilder.saveStatusHistory(
                deal, oldStatus, DealStatus.PAYMENT_PENDING, ownerId,
                DealChangeSource.USER, "Payment link created"
        );

        String confirmationUrl = response.getConfirmation() != null
                ? (String) response.getConfirmation().get("confirmation_url")
                : null;

        log.info("Payment {} created for deal {}", response.getId(), dealId);

        return PaymentConfirmationResponse.builder()
                .paymentId(response.getId())
                .confirmationUrl(confirmationUrl)
                .status(response.getStatus())
                .build();
    }

    @Override
    @Transactional
    public DealResponse handlePaymentSuccess(String yookassaPaymentId) {
        List<Transaction> transactions = transactionRepository.findAll();
        List<Transaction> paymentTransactions = transactions.stream()
                .filter(t -> yookassaPaymentId.equals(t.getYookassaPaymentId()))
                .toList();

        if (paymentTransactions.isEmpty()) {
            throw new DealNotFoundException("Transaction not found for payment: " + yookassaPaymentId);
        }

        Deal deal = paymentTransactions.get(0).getDeal();

        if (deal.getStatus() != DealStatus.PAYMENT_PENDING) {
            log.warn("Deal {} is not in PAYMENT_PENDING status", deal.getId());
            return dealResponseBuilder.buildDealResponse(deal);
        }

        paymentTransactions.forEach(t -> {
            t.setStatus(TransactionStatus.HELD);
            t.setUpdatedAt(OffsetDateTime.now());
        });

        DealStatus oldStatus = deal.getStatus();
        deal.setStatus(DealStatus.PAID);
        deal.setUpdatedAt(OffsetDateTime.now());

        dealResponseBuilder.saveStatusHistory(
                deal, oldStatus, DealStatus.PAID, null,
                DealChangeSource.PAYMENT_WEBHOOK, "Payment received"
        );

        log.info("Payment {} received for deal {}. Status changed to PAID", yookassaPaymentId, deal.getId());

        auditClient.sendLog("deal-payment-service", deal.getRenterId(), "renter",
                "PAYMENT_SUCCESS", "DEAL", deal.getId().toString(), null);

        return dealResponseBuilder.buildDealResponse(deal);
    }

    @Override
    @Transactional
    public DealResponse confirmStartDeal(UUID dealId, UUID userId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getOwnerId().equals(userId) && !deal.getRenterId().equals(userId)) {
            throw new DealAccessDeniedException("Only deal participants can confirm start");
        }

        if (deal.getStatus() != DealStatus.PAID) {
            throw new InvalidDealStatusException(
                    String.format("Cannot start deal with status '%s'. Expected PAID", deal.getStatus())
            );
        }

        if (dealConfirmationRepository.existsByDeal_IdAndUserIdAndAction(dealId, userId, "START")) {
            throw new IllegalArgumentException("You have already confirmed the start");
        }

        DealConfirmation confirmation = DealConfirmation.builder()
                .deal(deal)
                .userId(userId)
                .action("START")
                .confirmedAt(OffsetDateTime.now())
                .build();

        dealConfirmationRepository.save(confirmation);

        long confirmations = dealConfirmationRepository.countByDeal_IdAndAction(dealId, "START");

        if (confirmations >= 2) {
            DealStatus oldStatus = deal.getStatus();
            deal.setStatus(DealStatus.ACTIVE);
            deal.setUpdatedAt(OffsetDateTime.now());

            dealResponseBuilder.saveStatusHistory(
                    deal, oldStatus, DealStatus.ACTIVE, null,
                    DealChangeSource.SYSTEM, "Both parties confirmed start"
            );

            log.info("Deal {} started (both parties confirmed)", dealId);
        } else {
            log.info("Deal {} start confirmed by user {} (1/2)", dealId, userId);
        }

        auditClient.sendLog("deal-payment-service", userId, "user",
                "START_DEAL", "DEAL", dealId.toString(), null);

        return dealResponseBuilder.buildDealResponse(deal);
    }

    @Override
    @Transactional
    public DealResponse confirmCompleteDeal(UUID dealId, UUID userId, boolean itemOk) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getOwnerId().equals(userId) && !deal.getRenterId().equals(userId)) {
            throw new DealAccessDeniedException("Only deal participants can confirm completion");
        }

        if (deal.getStatus() != DealStatus.ACTIVE) {
            throw new InvalidDealStatusException(
                    String.format("Cannot complete deal with status '%s'", deal.getStatus())
            );
        }

        if (dealConfirmationRepository.existsByDeal_IdAndUserIdAndAction(dealId, userId, "COMPLETE")) {
            throw new IllegalArgumentException("You have already confirmed the completion");
        }

        DealConfirmation confirmation = DealConfirmation.builder()
                .deal(deal)
                .userId(userId)
                .action("COMPLETE")
                .confirmedAt(OffsetDateTime.now())
                .build();

        dealConfirmationRepository.save(confirmation);

        long confirmations = dealConfirmationRepository.countByDeal_IdAndAction(dealId, "COMPLETE");

        if (confirmations >= 2) {
            processDealCompletion(deal, itemOk);

            DealStatus oldStatus = deal.getStatus();
            deal.setStatus(DealStatus.COMPLETED);
            deal.setUpdatedAt(OffsetDateTime.now());

            dealResponseBuilder.saveStatusHistory(
                    deal, oldStatus, DealStatus.COMPLETED, null,
                    DealChangeSource.SYSTEM,
                    "Both parties confirmed completion. Item " + (itemOk ? "OK" : "DAMAGED")
            );

            log.info("Deal {} completed (both parties confirmed). Item OK: {}", dealId, itemOk);
        } else {
            log.info("Deal {} completion confirmed by user {} (1/2)", dealId, userId);
        }

        auditClient.sendLog("deal-payment-service", userId, "user",
                "COMPLETE_DEAL", "DEAL", dealId.toString(),
                "{\"itemOk\": " + itemOk + "}");

        return dealResponseBuilder.buildDealResponse(deal);
    }

    public void processDealCompletion(Deal deal, boolean itemOk) {
        List<Transaction> transactions = transactionRepository.findAllByDeal_Id(deal.getId());
        Transaction rentalTransaction = transactions.stream()
                .filter(t -> t.getType() == TransactionType.RENTAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Rental transaction not found"));

        if (properties.isMockEnabled()) {
            if (itemOk) {
                rentalTransaction.setStatus(TransactionStatus.CAPTURED);
                rentalTransaction.setUpdatedAt(OffsetDateTime.now());

                transactions.stream()
                        .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                        .findFirst()
                        .ifPresent(deposit -> {
                            deposit.setStatus(TransactionStatus.REFUNDED);
                            deposit.setUpdatedAt(OffsetDateTime.now());

                            Transaction release = Transaction.builder()
                                    .deal(deal)
                                    .type(TransactionType.DEPOSIT_RELEASE)
                                    .amount(deposit.getAmount())
                                    .status(TransactionStatus.REFUNDED)
                                    .createdAt(OffsetDateTime.now())
                                    .updatedAt(OffsetDateTime.now())
                                    .build();
                            transactionRepository.save(release);
                        });
            } else {
                BigDecimal fullAmount = deal.getTotalPrice().add(deal.getDepositAmount());
                rentalTransaction.setAmount(fullAmount);
                rentalTransaction.setStatus(TransactionStatus.CAPTURED);
                rentalTransaction.setUpdatedAt(OffsetDateTime.now());

                transactions.stream()
                        .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                        .forEach(t -> {
                            t.setStatus(TransactionStatus.CAPTURED);
                            t.setUpdatedAt(OffsetDateTime.now());
                        });
            }
            log.info("MOCK: Deal {} payment processed. itemOk={}", deal.getId(), itemOk);
            return;
        }

        String idempotenceKey = "capture_" + deal.getId() + "_" + System.currentTimeMillis();

        if (itemOk) {
            YooKassaCaptureRequest captureRequest = YooKassaCaptureRequest.builder()
                    .amount(YooKassaCaptureRequest.Amount.builder()
                            .value(deal.getTotalPrice().setScale(2, RoundingMode.HALF_UP).toString())
                            .currency("RUB")
                            .build())
                    .build();

            yooKassaClient.capturePayment(rentalTransaction.getYookassaPaymentId(), captureRequest, idempotenceKey);

            rentalTransaction.setStatus(TransactionStatus.CAPTURED);
            rentalTransaction.setUpdatedAt(OffsetDateTime.now());

            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                    .findFirst()
                    .ifPresent(depositTransaction -> {
                        YooKassaRefundRequest refundRequest = YooKassaRefundRequest.builder()
                                .paymentId(rentalTransaction.getYookassaPaymentId())
                                .amount(YooKassaRefundRequest.Amount.builder()
                                        .value(deal.getDepositAmount().setScale(2, RoundingMode.HALF_UP).toString())
                                        .currency("RUB")
                                        .build())
                                .description("Deposit refund for deal " + deal.getId())
                                .build();

                        yooKassaClient.createRefund(refundRequest, "refund_deposit_" + deal.getId());
                        depositTransaction.setStatus(TransactionStatus.REFUNDED);
                        depositTransaction.setUpdatedAt(OffsetDateTime.now());
                    });
        } else {
            BigDecimal fullAmount = deal.getTotalPrice().add(deal.getDepositAmount());

            YooKassaCaptureRequest captureRequest = YooKassaCaptureRequest.builder()
                    .amount(YooKassaCaptureRequest.Amount.builder()
                            .value(fullAmount.setScale(2, RoundingMode.HALF_UP).toString())
                            .currency("RUB")
                            .build())
                    .build();

            yooKassaClient.capturePayment(rentalTransaction.getYookassaPaymentId(), captureRequest, idempotenceKey);

            rentalTransaction.setAmount(fullAmount);
            rentalTransaction.setStatus(TransactionStatus.CAPTURED);
            rentalTransaction.setUpdatedAt(OffsetDateTime.now());

            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                    .forEach(t -> {
                        t.setStatus(TransactionStatus.CAPTURED);
                        t.setUpdatedAt(OffsetDateTime.now());
                    });
        }
    }

    @Override
    @Transactional
    public DealResponse cancelDealWithRefund(UUID dealId, UUID userId, String reason) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getOwnerId().equals(userId) && !deal.getRenterId().equals(userId)) {
            throw new DealAccessDeniedException("Only deal participants can cancel");
        }

        if (deal.getStatus() != DealStatus.ACTIVE) {
            throw new InvalidDealStatusException("Only active deal can be cancelled");
        }

        long totalDays = java.time.Duration.between(deal.getStartDate(), deal.getEndDate()).toDays();
        long usedDays = java.time.Duration.between(deal.getStartDate(), OffsetDateTime.now()).toDays();
        if (usedDays < 0) usedDays = 0;
        if (usedDays > totalDays) usedDays = totalDays;

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (totalDays > 0) {
            refundAmount = deal.getTotalPrice()
                    .multiply(BigDecimal.valueOf(totalDays - usedDays))
                    .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
        }

        List<Transaction> transactions = transactionRepository.findAllByDeal_Id(dealId);
        Transaction rentalTransaction = transactions.stream()
                .filter(t -> t.getType() == TransactionType.RENTAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Rental transaction not found"));

        if (properties.isMockEnabled()) {
            rentalTransaction.setStatus(TransactionStatus.CAPTURED);
            rentalTransaction.setUpdatedAt(OffsetDateTime.now());

            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                    .forEach(t -> {
                        t.setStatus(TransactionStatus.REFUNDED);
                        t.setUpdatedAt(OffsetDateTime.now());
                    });

            log.info("MOCK: Deal {} cancelled. Used {} of {} days.", deal.getId(), usedDays, totalDays);

            DealStatus oldStatus = deal.getStatus();
            deal.setStatus(DealStatus.CANCELLED);
            deal.setUpdatedAt(OffsetDateTime.now());

            dealResponseBuilder.saveStatusHistory(
                    deal, oldStatus, DealStatus.CANCELLED, userId,
                    DealChangeSource.USER, reason + " | Used " + usedDays + " of " + totalDays + " days (MOCK)"
            );

            return dealResponseBuilder.buildDealResponse(deal);
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            YooKassaRefundRequest refundRequest = YooKassaRefundRequest.builder()
                    .paymentId(rentalTransaction.getYookassaPaymentId())
                    .amount(YooKassaRefundRequest.Amount.builder()
                            .value(refundAmount.setScale(2, RoundingMode.HALF_UP).toString())
                            .currency("RUB")
                            .build())
                    .description("Partial refund for deal " + dealId)
                    .build();

            yooKassaClient.createRefund(refundRequest, "refund_cancel_" + dealId);
        }

        BigDecimal usedAmount = deal.getTotalPrice().subtract(refundAmount);
        if (usedAmount.compareTo(BigDecimal.ZERO) > 0) {
            YooKassaCaptureRequest captureRequest = YooKassaCaptureRequest.builder()
                    .amount(YooKassaCaptureRequest.Amount.builder()
                            .value(usedAmount.setScale(2, RoundingMode.HALF_UP).toString())
                            .currency("RUB")
                            .build())
                    .build();

            yooKassaClient.capturePayment(rentalTransaction.getYookassaPaymentId(), captureRequest,
                    "capture_cancel_" + dealId);
        }

        rentalTransaction.setStatus(TransactionStatus.CAPTURED);
        rentalTransaction.setUpdatedAt(OffsetDateTime.now());

        transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                .forEach(t -> {
                    YooKassaRefundRequest refundRequest = YooKassaRefundRequest.builder()
                            .paymentId(rentalTransaction.getYookassaPaymentId())
                            .amount(YooKassaRefundRequest.Amount.builder()
                                    .value(t.getAmount().setScale(2, RoundingMode.HALF_UP).toString())
                                    .currency("RUB")
                                    .build())
                            .description("Deposit refund for cancelled deal " + dealId)
                            .build();

                    yooKassaClient.createRefund(refundRequest, "refund_deposit_cancel_" + dealId);
                    t.setStatus(TransactionStatus.REFUNDED);
                    t.setUpdatedAt(OffsetDateTime.now());
                });

        DealStatus oldStatus = deal.getStatus();
        deal.setStatus(DealStatus.CANCELLED);
        deal.setUpdatedAt(OffsetDateTime.now());

        dealResponseBuilder.saveStatusHistory(
                deal, oldStatus, DealStatus.CANCELLED, userId,
                DealChangeSource.USER, reason + " | Used " + usedDays + " of " + totalDays + " days"
        );

        return dealResponseBuilder.buildDealResponse(deal);
    }

    @Override
    @Transactional
    public DealResponse cancelDealWithFullRefund(UUID dealId, UUID userId, String reason) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getOwnerId().equals(userId) && !deal.getRenterId().equals(userId)) {
            throw new DealAccessDeniedException("Only deal participants can cancel");
        }

        if (deal.getStatus() != DealStatus.PAID) {
            throw new InvalidDealStatusException("Only paid deal can be fully refunded");
        }

        List<Transaction> transactions = transactionRepository.findAllByDeal_Id(dealId);
        Transaction rentalTransaction = transactions.stream()
                .filter(t -> t.getType() == TransactionType.RENTAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Rental transaction not found"));

        if (properties.isMockEnabled()) {
            rentalTransaction.setStatus(TransactionStatus.REFUNDED);
            rentalTransaction.setUpdatedAt(OffsetDateTime.now());

            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                    .forEach(t -> {
                        t.setStatus(TransactionStatus.REFUNDED);
                        t.setUpdatedAt(OffsetDateTime.now());
                    });

            log.info("MOCK: Deal {} fully refunded", dealId);
        } else {
            // Полный возврат через ЮKassa
            YooKassaRefundRequest refundRequest = YooKassaRefundRequest.builder()
                    .paymentId(rentalTransaction.getYookassaPaymentId())
                    .amount(YooKassaRefundRequest.Amount.builder()
                            .value(deal.getTotalPrice().add(deal.getDepositAmount())
                                    .setScale(2, RoundingMode.HALF_UP).toString())
                            .currency("RUB")
                            .build())
                    .description("Full refund for deal " + dealId)
                    .build();

            yooKassaClient.createRefund(refundRequest, "full_refund_" + dealId);

            rentalTransaction.setStatus(TransactionStatus.REFUNDED);
            rentalTransaction.setUpdatedAt(OffsetDateTime.now());

            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.DEPOSIT_HOLD)
                    .forEach(t -> {
                        t.setStatus(TransactionStatus.REFUNDED);
                        t.setUpdatedAt(OffsetDateTime.now());
                    });
        }

        DealStatus oldStatus = deal.getStatus();
        deal.setStatus(DealStatus.CANCELLED);
        deal.setUpdatedAt(OffsetDateTime.now());

        dealResponseBuilder.saveStatusHistory(
                deal, oldStatus, DealStatus.CANCELLED, userId,
                DealChangeSource.USER, reason + " | Full refund"
        );

        return dealResponseBuilder.buildDealResponse(deal);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectToMap(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return Map.of("raw", obj.toString());
    }
}
