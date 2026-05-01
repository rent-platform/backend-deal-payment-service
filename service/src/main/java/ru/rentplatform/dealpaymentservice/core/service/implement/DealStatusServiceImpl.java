package ru.rentplatform.dealpaymentservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.request.CancelDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.request.RejectDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.api.exception.InvalidDealStatusException;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealChangeSource;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.service.DealStatusService;
import ru.rentplatform.dealpaymentservice.core.util.DealResponseBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealStatusServiceImpl implements DealStatusService {

    private final DealRepository dealRepository;
    private final DealResponseBuilder dealResponseBuilder;

    @Override
    @Transactional
    public DealResponse confirmDeal(UUID ownerId, UUID dealId) {
        Deal deal = getDeal(dealId);

        if (!deal.getOwnerId().equals(ownerId)) {
            throw new DealAccessDeniedException("Only owner can confirm deal");
        }

        if (deal.getStatus() != DealStatus.PENDING) {
            throw new InvalidDealStatusException("Only pending deal can be confirmed");
        }

        // Находим все конфликтующие PENDING-сделки на те же даты
        List<Deal> conflictingDeals = dealRepository.findConflictingPendingDeals(
                deal.getItemId(),
                deal.getStartDate(),
                deal.getEndDate(),
                deal.getId()
        );

        OffsetDateTime now = OffsetDateTime.now();

        // Отклоняем конфликтующие сделки
        for (Deal conflicting : conflictingDeals) {
            DealStatus oldConflictingStatus = conflicting.getStatus();

            conflicting.setStatus(DealStatus.REJECTED);
            conflicting.setRejectionReason("Another deal confirmed for these dates");
            conflicting.setUpdatedAt(now);

            dealResponseBuilder.saveStatusHistory(
                    conflicting,
                    oldConflictingStatus,
                    DealStatus.REJECTED,
                    null,
                    DealChangeSource.SYSTEM,
                    "Rejected due to confirmation of deal " + dealId
            );
        }

        // Подтверждаем текущую сделку
        DealStatus oldStatus = deal.getStatus();
        deal.setStatus(DealStatus.CONFIRMED);
        deal.setUpdatedAt(now);

        dealResponseBuilder.saveStatusHistory(
                deal,
                oldStatus,
                DealStatus.CONFIRMED,
                ownerId,
                DealChangeSource.USER,
                "Deal confirmed"
        );

        log.info("Deal {} confirmed by owner {}. Rejected {} conflicting deals.",
                dealId, ownerId, conflictingDeals.size());

        return dealResponseBuilder.buildDealResponse(deal);
    }

    @Override
    @Transactional
    public DealResponse rejectDeal(UUID ownerId, UUID dealId, RejectDealRequest request) {
        Deal deal = getDeal(dealId);

        if (!deal.getOwnerId().equals(ownerId)) {
            throw new DealAccessDeniedException("Only owner can reject deal");
        }

        if (deal.getStatus() != DealStatus.PENDING) {
            throw new InvalidDealStatusException("Only pending deal can be rejected");
        }

        DealStatus oldStatus = deal.getStatus();

        deal.setStatus(DealStatus.REJECTED);
        deal.setRejectionReason(request.getReason());
        deal.setUpdatedAt(OffsetDateTime.now());

        Deal savedDeal = dealRepository.save(deal);

        dealResponseBuilder.saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.REJECTED,
                ownerId,
                DealChangeSource.USER,
                request.getReason()
        );

        return dealResponseBuilder.buildDealResponse(savedDeal);
    }

    @Override
    @Transactional
    public DealResponse cancelDeal(UUID currentUserId, UUID dealId, CancelDealRequest request) {
        Deal deal = getDeal(dealId);

        if (!deal.getOwnerId().equals(currentUserId) && !deal.getRenterId().equals(currentUserId)) {
            throw new DealAccessDeniedException("Only deal participant can cancel deal");
        }

        if (deal.getStatus() != DealStatus.PENDING && deal.getStatus() != DealStatus.CONFIRMED) {
            throw new InvalidDealStatusException("Only pending or confirmed deal can be cancelled");
        }

        DealStatus oldStatus = deal.getStatus();

        deal.setStatus(DealStatus.CANCELLED);
        deal.setUpdatedAt(OffsetDateTime.now());

        Deal savedDeal = dealRepository.save(deal);

        dealResponseBuilder.saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.CANCELLED,
                currentUserId,
                DealChangeSource.USER,
                request.getReason()
        );

        return dealResponseBuilder.buildDealResponse(savedDeal);
    }

    @Override
    @Transactional
    public DealResponse startDeal(UUID ownerId, UUID dealId) {
        Deal deal = getDeal(dealId);

        if (!deal.getOwnerId().equals(ownerId)) {
            throw new DealAccessDeniedException("Only owner can start deal");
        }

        if (deal.getStatus() != DealStatus.CONFIRMED) {
            throw new InvalidDealStatusException("Only confirmed deal can be started");
        }

        DealStatus oldStatus = deal.getStatus();

        deal.setStatus(DealStatus.ACTIVE);
        deal.setUpdatedAt(OffsetDateTime.now());

        Deal savedDeal = dealRepository.save(deal);

        dealResponseBuilder.saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.ACTIVE,
                ownerId,
                DealChangeSource.USER,
                "Deal started"
        );

        return dealResponseBuilder.buildDealResponse(savedDeal);
    }

    @Override
    @Transactional
    public DealResponse completeDeal(UUID ownerId, UUID dealId) {
        Deal deal = getDeal(dealId);

        if (!deal.getOwnerId().equals(ownerId)) {
            throw new DealAccessDeniedException("Only owner can complete deal");
        }

        if (deal.getStatus() != DealStatus.ACTIVE) {
            throw new InvalidDealStatusException("Only active deal can be completed");
        }

        DealStatus oldStatus = deal.getStatus();

        deal.setStatus(DealStatus.COMPLETED);
        deal.setUpdatedAt(OffsetDateTime.now());

        Deal savedDeal = dealRepository.save(deal);

        dealResponseBuilder.saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.COMPLETED,
                ownerId,
                DealChangeSource.USER,
                "Deal completed"
        );

        return dealResponseBuilder.buildDealResponse(savedDeal);
    }

    private Deal getDeal(UUID dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));
    }
}
