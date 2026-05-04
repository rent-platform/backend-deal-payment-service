package ru.rentplatform.dealpaymentservice.core.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "deal_confirmations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealConfirmation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "confirmed_at", nullable = false)
    private OffsetDateTime confirmedAt;
}
