package ru.rentplatform.dealpaymentservice.client.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatServiceClient {

    private final RestClient chatServiceRestClient;

    public void sendDealStatusMessage(UUID dealId, UUID itemId, String status) {
        try {
            chatServiceRestClient.post()
                    .uri("/api/internal/chats/deal-status")
                    .body(Map.of(
                            "itemId", itemId.toString(),
                            "dealId", dealId.toString(),
                            "status", status
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send deal status to chat: {}", e.getMessage());
        }
    }
}
