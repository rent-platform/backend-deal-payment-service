package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.PaymentConfirmationResponse;
import ru.rentplatform.dealpaymentservice.core.service.PaymentService;

import java.util.UUID;

import static ru.rentplatform.dealpaymentservice.api.ApiPaths.DEALS;

@RestController
@RequestMapping(DEALS)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Оплата (ЮKassa)", description = "Создание платежей")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{dealId}/payment")
    @Operation(summary = "Создать ссылку на оплату", description = "Владелец создаёт платёж в ЮKassa." +
            "Деньги холдируются до завершения сделки")
    public PaymentConfirmationResponse createPayment(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID dealId) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return paymentService.createPayment(dealId, ownerId);
    }
}
