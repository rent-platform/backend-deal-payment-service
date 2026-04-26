package ru.rentplatform.dealpaymentservice.core.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "deal_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealStatusHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private DealStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private DealStatus newStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_source", nullable = false, length = 20)
    private DealChangeSource changeSource;

    @Column(name = "comment")
    private String comment;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;
}
