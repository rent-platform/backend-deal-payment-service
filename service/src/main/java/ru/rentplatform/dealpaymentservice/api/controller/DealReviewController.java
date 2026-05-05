package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealReviewRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealReviewResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.ItemRatingSummaryResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.UserRatingSummaryResponse;
import ru.rentplatform.dealpaymentservice.core.service.DealReviewService;

import java.util.List;
import java.util.UUID;

import static ru.rentplatform.dealpaymentservice.api.ApiPaths.DEALS;

@RestController
@RequiredArgsConstructor
@Tag(name = "Отзывы", description = "Отзывы о сделках аренды")
public class DealReviewController {

    private final DealReviewService dealReviewService;

    @PostMapping(DEALS + "/{dealId}/review")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Оставить отзыв", description = "Участник сделки оставляет отзыв. Только после COMPLETED")
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
    @Operation(summary = "Отзывы по сделке", description = "Получить все отзывы по конкретной сделке")
    public List<DealReviewResponse> getDealReviews(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID dealId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return dealReviewService.getDealReviews(currentUserId, dealId);
    }

    @GetMapping("/api/reviews/users/{userId}")
    @Operation(summary = "Отзывы о пользователе", description = "Публичный список отзывов о пользователе")
    public Page<DealReviewResponse> getUserReviews(
            @PathVariable UUID userId,
            Pageable pageable
    ) {
        return dealReviewService.getUserReviews(userId, pageable);
    }

    @GetMapping("/api/reviews/items/{itemId}")
    @Operation(summary = "Отзывы о товаре", description = "Публичный список отзывов о товаре")
    public Page<DealReviewResponse> getItemReviews(
            @PathVariable UUID itemId,
            Pageable pageable
    ) {
        return dealReviewService.getItemReviews(itemId, pageable);
    }

    @GetMapping("/api/reviews/users/{userId}/summary")
    @Operation(summary = "Рейтинг пользователя",
            description = "Общий рейтинг, рейтинг как арендодатель и как арендатор")
    public UserRatingSummaryResponse getUserRatingSummary(@PathVariable UUID userId) {
        return dealReviewService.getUserRatingSummary(userId);
    }

    @GetMapping("/api/reviews/items/{itemId}/summary")
    @Operation(summary = "Рейтинг товара",
            description = "Средний рейтинг и количество отзывов о товаре")
    public ItemRatingSummaryResponse getItemRatingSummary(@PathVariable UUID itemId) {
        return dealReviewService.getItemRatingSummary(itemId);
    }
}
