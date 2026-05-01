package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealReviewRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealReviewResponse;
import ru.rentplatform.dealpaymentservice.core.service.DealReviewService;

import java.util.List;
import java.util.UUID;

import static ru.rentplatform.dealpaymentservice.api.ApiPaths.DEALS;

@RestController
@RequiredArgsConstructor
public class DealReviewController {

    private final DealReviewService dealReviewService;

    @PostMapping(DEALS + "/{dealId}/review")
    @SecurityRequirement(name = "bearerAuth")
    public DealReviewResponse createReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID dealId,
            @Valid @RequestBody CreateDealReviewRequest request
    ) {
        UUID reviewerId = UUID.fromString(jwt.getSubject());
        return dealReviewService.createReview(reviewerId, dealId, request);
    }

    @GetMapping(DEALS + "/{dealId}/reviews")
    @SecurityRequirement(name = "bearerAuth")
    public List<DealReviewResponse> getDealReviews(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID dealId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return dealReviewService.getDealReviews(currentUserId, dealId);
    }

    @GetMapping("/api/reviews/users/{userId}")
    public Page<DealReviewResponse> getUserReviews(
            @PathVariable UUID userId,
            Pageable pageable
    ) {
        return dealReviewService.getUserReviews(userId, pageable);
    }

    @GetMapping("/api/reviews/items/{itemId}")
    public Page<DealReviewResponse> getItemReviews(
            @PathVariable UUID itemId,
            Pageable pageable
    ) {
        return dealReviewService.getItemReviews(itemId, pageable);
    }
}
