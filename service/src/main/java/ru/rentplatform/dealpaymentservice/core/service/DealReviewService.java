package ru.rentplatform.dealpaymentservice.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealReviewRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealReviewResponse;

import java.util.List;
import java.util.UUID;

public interface DealReviewService {

    DealReviewResponse createReview(UUID reviewerId, UUID dealId, CreateDealReviewRequest request);

    List<DealReviewResponse> getDealReviews(UUID currentUserId, UUID dealId);

    Page<DealReviewResponse> getUserReviews(UUID userId, Pageable pageable);

    Page<DealReviewResponse> getItemReviews(UUID itemId, Pageable pageable);
}