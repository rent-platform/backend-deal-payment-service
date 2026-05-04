package ru.rentplatform.dealpaymentservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.rentplatform.dealpaymentservice.client.yookassa.dto.*;
import ru.rentplatform.dealpaymentservice.config.YooKassaProperties;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class YooKassaClient {

    private final YooKassaProperties properties;

    private RestClient buildClient() {
        String auth = properties.getShopId() + ":" + properties.getSecretKey();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        return RestClient.builder()
                .baseUrl("https://api.yookassa.ru/v3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public YooKassaPaymentResponse createPayment(YooKassaPaymentRequest request, String idempotenceKey) {
        return buildClient().post()
                .uri("/payments")
                .header("Idempotence-Key", idempotenceKey)
                .body(request)
                .retrieve()
                .body(YooKassaPaymentResponse.class);
    }

    public YooKassaPaymentResponse getPayment(String paymentId) {
        return buildClient().get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .body(YooKassaPaymentResponse.class);
    }

    public YooKassaPaymentResponse capturePayment(String paymentId, YooKassaCaptureRequest request, String idempotenceKey) {
        return buildClient().post()
                .uri("/payments/{paymentId}/capture", paymentId)
                .header("Idempotence-Key", idempotenceKey)
                .body(request)
                .retrieve()
                .body(YooKassaPaymentResponse.class);
    }

    public YooKassaPaymentResponse cancelPayment(String paymentId, String idempotenceKey) {
        return buildClient().post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header("Idempotence-Key", idempotenceKey)
                .retrieve()
                .body(YooKassaPaymentResponse.class);
    }

    public YooKassaRefundResponse createRefund(YooKassaRefundRequest request, String idempotenceKey) {
        return buildClient().post()
                .uri("/refunds")
                .header("Idempotence-Key", idempotenceKey)
                .body(request)
                .retrieve()
                .body(YooKassaRefundResponse.class);
    }
}
