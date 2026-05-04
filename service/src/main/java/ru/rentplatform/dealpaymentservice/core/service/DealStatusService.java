package ru.rentplatform.dealpaymentservice.core.service;

import ru.rentplatform.dealpaymentservice.api.dto.request.CancelDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.request.RejectDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;

import java.util.UUID;

public interface DealStatusService {

    DealResponse confirmDeal(UUID ownerId, UUID dealId);

    DealResponse rejectDeal(UUID ownerId, UUID dealId, RejectDealRequest request);

    DealResponse cancelDeal(UUID currentUserId, UUID dealId, CancelDealRequest request);

    DealResponse confirmStartDeal(UUID dealId, UUID userId);

    DealResponse confirmCompleteDeal(UUID dealId, UUID userId, boolean itemOk);
}
