package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealReviewResponse {

    private UUID id;

    private UUID dealId;

    private UUID itemId;

    private UUID reviewerId;

    private UUID reviewedUserId;

    private String reviewType;

    private Integer rating;

    private String text;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
