package ru.rentplatform.dealpaymentservice.client.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditClient {

    private final RestClient auditServiceRestClient;

    public void sendLog(String service, UUID userId, String nickname, String action,
                        String targetType, String targetId, String details) {
        try {
            auditServiceRestClient.post()
                    .uri("/api/internal/audit")
                    .body(Map.of(
                            "service", service,
                            "userId", userId.toString(),
                            "nickname", nickname,
                            "action", action,
                            "targetType", targetType != null ? targetType : "",
                            "targetId", targetId != null ? targetId.toString() : "",
                            "details", details != null ? details : ""
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send audit log: {}", e.getMessage());
        }
    }
}
