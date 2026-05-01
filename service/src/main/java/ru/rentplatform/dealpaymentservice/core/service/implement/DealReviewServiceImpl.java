package ru.rentplatform.dealpaymentservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealReviewRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealReviewResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.core.dao.entity.*;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealReviewRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;
import ru.rentplatform.dealpaymentservice.core.service.DealReviewService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealReviewServiceImpl implements DealReviewService {

    private final DealRepository dealRepository;
    private final DealReviewRepository dealReviewRepository;
    private final DealMapper dealMapper;

    @Override
    @Transactional
    public DealReviewResponse createReview(UUID reviewerId, UUID dealId, CreateDealReviewRequest request) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (deal.getStatus() != DealStatus.COMPLETED) {
            throw new IllegalArgumentException("Review can be created only after deal is completed");
        }

        if (!deal.getRenterId().equals(reviewerId) && !deal.getOwnerId().equals(reviewerId)) {
            throw new DealAccessDeniedException("Only deal participant can leave review");
        }

        boolean alreadyReviewed = dealReviewRepository.existsByDeal_IdAndReviewerId(dealId, reviewerId);
        if (alreadyReviewed) {
            throw new IllegalArgumentException("You have already reviewed this deal");
        }

        UUID reviewedUserId;
        DealReviewType reviewType;

        if (deal.getRenterId().equals(reviewerId)) {
            reviewedUserId = deal.getOwnerId();
            reviewType = DealReviewType.RENTER_TO_OWNER;
        } else {
            reviewedUserId = deal.getRenterId();
            reviewType = DealReviewType.OWNER_TO_RENTER;
        }

        OffsetDateTime now = OffsetDateTime.now();

        DealReview review = DealReview.builder()
                .deal(deal)
                .itemId(deal.getItemId())
                .reviewerId(reviewerId)
                .reviewedUserId(reviewedUserId)
                .reviewType(reviewType)
                .rating(request.getRating())
                .text(normalizeText(request.getText()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        DealReview savedReview = dealReviewRepository.save(review);
        return dealMapper.toDealReviewResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealReviewResponse> getDealReviews(UUID currentUserId, UUID dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getRenterId().equals(currentUserId) && !deal.getOwnerId().equals(currentUserId)) {
            throw new DealAccessDeniedException("Access denied");
        }

        return dealReviewRepository.findAllByDeal_IdOrderByCreatedAtAsc(dealId)
                .stream()
                .map(dealMapper::toDealReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DealReviewResponse> getUserReviews(UUID userId, Pageable pageable) {
        return dealReviewRepository.findAllByReviewedUserId(userId, pageable)
                .map(dealMapper::toDealReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DealReviewResponse> getItemReviews(UUID itemId, Pageable pageable) {
        return dealReviewRepository.findAllByItemIdAndReviewType(
                        itemId,
                        DealReviewType.RENTER_TO_OWNER,
                        pageable
                )
                .map(dealMapper::toDealReviewResponse);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }

        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
