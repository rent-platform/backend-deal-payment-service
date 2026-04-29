package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealResponse {

    private UUID id;

    private UUID itemId;

    private UUID renterId;

    private UUID ownerId;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private String pricingMode;

    private BigDecimal pricePerDaySnapshot;

    private BigDecimal pricePerHourSnapshot;

    private BigDecimal totalPrice;

    private BigDecimal depositAmount;

    private String status;

    private String rejectionReason;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private List<DealStatusHistoryResponse> history;
}
