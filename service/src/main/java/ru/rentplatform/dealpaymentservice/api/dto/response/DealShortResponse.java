package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealShortResponse {

    private UUID id;

    private UUID itemId;

    private UUID renterId;

    private UUID ownerId;

    private String status;

    private String pricingMode;

    private BigDecimal totalPrice;

    private BigDecimal depositAmount;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private OffsetDateTime createdAt;
}