package ru.rentplatform.dealpaymentservice.core.service;

import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.PaymentConfirmationResponse;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;

import java.util.UUID;

public interface PaymentService {

    PaymentConfirmationResponse createPayment(UUID dealId, UUID ownerId);

    DealResponse handlePaymentSuccess(String yookassaPaymentId);

    DealResponse confirmStartDeal(UUID dealId, UUID userId);

    DealResponse confirmCompleteDeal(UUID dealId, UUID userId, boolean itemOk);

    DealResponse cancelDealWithRefund(UUID dealId, UUID userId, String reason);

    DealResponse cancelDealWithFullRefund(UUID dealId, UUID userId, String reason);

    void processDealCompletion(Deal deal, boolean itemOk);
}