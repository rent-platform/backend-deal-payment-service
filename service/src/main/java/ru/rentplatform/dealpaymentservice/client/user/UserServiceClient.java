package ru.rentplatform.dealpaymentservice.client.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    public void updateBillingProfile(UUID userId, String customerId, String paymentMethodId) {
        try {
            userServiceRestClient.put()
                    .uri("/api/internal/users/{userId}/billing", userId)
                    .body(Map.of(
                            "customerId", customerId != null ? customerId : "",
                            "paymentMethodId", paymentMethodId != null ? paymentMethodId : ""
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
        }
    }
}