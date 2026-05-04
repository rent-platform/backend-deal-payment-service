package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.core.service.PaymentService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook ЮKassa", description = "Приём уведомлений от ЮKassa о статусе платежей")
public class YooKassaWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/yookassa")
    @Operation(summary = "Webhook ЮKassa",
            description = "Принимает уведомления об оплате. При payment.succeeded обновляет статус транзакций на HELD")
    public void handleYookassaWebhook(@RequestBody Map<String, Object> payload) {
        log.info("YooKassa webhook received: {}", payload);

        String event = (String) payload.get("event");
        Map<String, Object> object = (Map<String, Object>) payload.get("object");

        if (object == null) return;

        String paymentId = (String) object.get("id");
        String status = (String) object.get("status");

        if ("payment.succeeded".equals(event) && "succeeded".equals(status)) {
            paymentService.handlePaymentSuccess(paymentId);
        }
    }
}
