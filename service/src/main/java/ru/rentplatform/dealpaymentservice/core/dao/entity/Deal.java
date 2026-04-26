package ru.rentplatform.dealpaymentservice.core.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "deals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deal {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "renter_id", nullable = false)
    private UUID renterId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 10)
    private PricingMode pricingMode;

    @Column(name = "price_per_day_snapshot", precision = 10, scale = 2)
    private BigDecimal pricePerDaySnapshot;

    @Column(name = "price_per_hour_snapshot", precision = 10, scale = 2)
    private BigDecimal pricePerHourSnapshot;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DealStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
