package ru.rentplatform.dealpaymentservice.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Жалоба")
public class ComplaintResponse {

    private UUID id;

    private UUID authorId;

    private String targetType;

    private UUID targetId;

    private String reason;

    private String status;

    private UUID handledBy;

    private String resolution;

    private OffsetDateTime createdAt;
}
