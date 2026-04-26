package ru.rentplatform.dealpaymentservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.request.CancelDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.request.RejectDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealCommentResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealStatusHistoryResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.api.exception.InvalidDealStatusException;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealChangeSource;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatusHistory;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealCommentRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealStatusHistoryRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;
import ru.rentplatform.dealpaymentservice.core.service.DealStatusService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealStatusServiceImpl implements DealStatusService {

    private final DealRepository dealRepository;
    private final DealCommentRepository dealCommentRepository;
    private final DealStatusHistoryRepository dealStatusHistoryRepository;
    private final DealMapper dealMapper;

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

        DealStatus oldStatus = deal.getStatus();

        deal.setStatus(DealStatus.CONFIRMED);
        deal.setUpdatedAt(OffsetDateTime.now());

        Deal savedDeal = dealRepository.save(deal);

        saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.CONFIRMED,
                ownerId,
                DealChangeSource.USER,
                "Deal confirmed"
        );

        return buildDealResponse(savedDeal);
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

        saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.REJECTED,
                ownerId,
                DealChangeSource.USER,
                request.getReason()
        );

        return buildDealResponse(savedDeal);
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

        saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.CANCELLED,
                currentUserId,
                DealChangeSource.USER,
                request.getReason()
        );

        return buildDealResponse(savedDeal);
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

        saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.ACTIVE,
                ownerId,
                DealChangeSource.USER,
                "Deal started"
        );

        return buildDealResponse(savedDeal);
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

        saveStatusHistory(
                savedDeal,
                oldStatus,
                DealStatus.COMPLETED,
                ownerId,
                DealChangeSource.USER,
                "Deal completed"
        );

        return buildDealResponse(savedDeal);
    }

    private Deal getDeal(UUID dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));
    }

    private void saveStatusHistory(Deal deal,
                                   DealStatus oldStatus,
                                   DealStatus newStatus,
                                   UUID changedBy,
                                   DealChangeSource changeSource,
                                   String comment) {
        DealStatusHistory history = DealStatusHistory.builder()
                .id(UUID.randomUUID())
                .deal(deal)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .changeSource(changeSource)
                .comment(comment)
                .changedAt(OffsetDateTime.now())
                .build();

        dealStatusHistoryRepository.save(history);
    }

    private DealResponse buildDealResponse(Deal deal) {
        List<DealCommentResponse> comments = dealCommentRepository.findAllByDeal_IdOrderByCreatedAtAsc(deal.getId())
                .stream()
                .map(dealMapper::toDealCommentResponse)
                .toList();

        List<DealStatusHistoryResponse> history = dealStatusHistoryRepository.findAllByDeal_IdOrderByChangedAtAsc(deal.getId())
                .stream()
                .map(dealMapper::toDealStatusHistoryResponse)
                .toList();

        DealResponse response = dealMapper.toDealResponse(deal);
        response.setComments(comments);
        response.setHistory(history);

        return response;
    }
}
