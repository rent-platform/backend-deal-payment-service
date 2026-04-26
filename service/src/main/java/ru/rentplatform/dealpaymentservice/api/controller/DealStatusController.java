package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.request.CancelDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.request.RejectDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.core.service.DealStatusService;

import java.util.UUID;

import static ru.rentplatform.dealpaymentservice.api.ApiPaths.DEALS;

@RestController
@RequestMapping(DEALS)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DealStatusController {

    private final DealStatusService dealStatusService;

    @PostMapping("/{dealId}/confirm")
    public DealResponse confirmDeal(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID dealId) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealStatusService.confirmDeal(ownerId, dealId);
    }

    @PostMapping("/{dealId}/reject")
    public DealResponse rejectDeal(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID dealId,
                                   @Valid @RequestBody RejectDealRequest request) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealStatusService.rejectDeal(ownerId, dealId, request);
    }

    @PostMapping("/{dealId}/cancel")
    public DealResponse cancelDeal(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID dealId,
                                   @Valid @RequestBody CancelDealRequest request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return dealStatusService.cancelDeal(currentUserId, dealId, request);
    }

    @PostMapping("/{dealId}/start")
    public DealResponse startDeal(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID dealId) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealStatusService.startDeal(ownerId, dealId);
    }

    @PostMapping("/{dealId}/complete")
    public DealResponse completeDeal(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID dealId) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealStatusService.completeDeal(ownerId, dealId);
    }
}
