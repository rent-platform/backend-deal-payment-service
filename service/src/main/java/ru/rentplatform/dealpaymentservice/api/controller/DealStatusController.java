package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Статусы сделок", description = "Управление жизненным циклом сделки")
public class DealStatusController {

    private final DealStatusService dealStatusService;

    @PostMapping("/{dealId}/confirm")
    @Operation(summary = "Подтвердить сделку", description = "Арендодатель подтверждает сделку. " +
            "Конфликтующие PENDING-сделки отклоняются автоматически")
    public DealResponse confirmDeal(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID dealId) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealStatusService.confirmDeal(ownerId, dealId);
    }

    @PostMapping("/{dealId}/reject")
    @Operation(summary = "Отклонить сделку", description = "Арендодатель отклоняет сделку с указанием причины")
    public DealResponse rejectDeal(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID dealId,
                                   @Valid @RequestBody RejectDealRequest request) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return dealStatusService.rejectDeal(ownerId, dealId, request);
    }

    @PostMapping("/{dealId}/cancel")
    @Operation(summary = "Отменить сделку", description = "Любая сторона отменяет " +
            "сделку (PENDING / CONFIRMED / PAYMENT_PENDING)")
    public DealResponse cancelDeal(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable UUID dealId,
                                   @Valid @RequestBody CancelDealRequest request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return dealStatusService.cancelDeal(currentUserId, dealId, request);
    }

    @PostMapping("/{dealId}/confirm-start")
    @Operation(summary = "Подтвердить старт аренды",
            description = "Обе стороны должны подтвердить старт для начала аренды")
    public DealResponse confirmStart(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID dealId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return dealStatusService.confirmStartDeal(dealId, userId);
    }

    @PostMapping("/{dealId}/confirm-complete")
    @Operation(summary = "Подтвердить завершение аренды",
            description = "Обе стороны должны подтвердить завершение для окончания аренды")
    public DealResponse confirmComplete(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID dealId,
                                        @RequestParam(defaultValue = "true") boolean itemOk) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return dealStatusService.confirmCompleteDeal(dealId, userId, itemOk);
    }
}
