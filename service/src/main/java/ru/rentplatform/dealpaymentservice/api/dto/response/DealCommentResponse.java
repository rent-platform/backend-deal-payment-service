package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealCommentResponse {

    private UUID id;

    private UUID authorId;

    private String text;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
