package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealShortResponse;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;
import ru.rentplatform.dealpaymentservice.core.service.DealService;

import java.util.UUID;

import static ru.rentplatform.dealpaymentservice.api.ApiPaths.DEALS;

@RestController
@RequestMapping(DEALS)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DealController {

    private final DealService dealService;

    @PostMapping
    public DealResponse createDeal(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody CreateDealRequest request) {
        UUID renterId = UUID.fromString(jwt.getSubject());
        return dealService.createDeal(renterId, request);
    }

    @GetMapping("/{dealId}")
    public DealResponse getDealById(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID dealId) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return dealService.getDealById(currentUserId, dealId);
    }

    @GetMapping("/my/renter")
    public Page<DealShortResponse> getMyRenterDeals(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) DealStatus status,
            Pageable pageable
    ) {
        UUID renterId = UUID.fromString(jwt.getSubject());
        return dealService.getMyRenterDeals(renterId, status, pageable);
    }

    @GetMapping("/my/owner")
    public Page<DealShortResponse> getMyOwnerDeals(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) DealStatus status,
            Pageable pageable
    ) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealService.getMyOwnerDeals(ownerId, status, pageable);
    }
}
