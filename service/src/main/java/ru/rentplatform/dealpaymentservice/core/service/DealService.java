package ru.rentplatform.dealpaymentservice.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealShortResponse;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;

import java.util.UUID;

public interface DealService {

    DealResponse createDeal(UUID renterId, CreateDealRequest request);

    DealResponse getDealById(UUID currentUserId, UUID dealId);

    Page<DealShortResponse> getMyRenterDeals(UUID renterId, DealStatus status, Pageable pageable);

    Page<DealShortResponse> getMyOwnerDeals(UUID ownerId, DealStatus status, Pageable pageable);
}
