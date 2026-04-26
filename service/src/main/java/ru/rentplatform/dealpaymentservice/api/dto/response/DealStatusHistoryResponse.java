package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealStatusHistoryResponse {

    private UUID id;

    private UUID dealId;

    private String oldStatus;

    private String newStatus;

    private UUID changedBy;

    private String changeSource;

    private String comment;

    private OffsetDateTime changedAt;
}
