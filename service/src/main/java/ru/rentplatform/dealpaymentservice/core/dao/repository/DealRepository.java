package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DealRepository extends JpaRepository<Deal, UUID> {

    Optional<Deal> findByIdAndRenterId(UUID id, UUID renterId);

    Optional<Deal> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Deal> findAllByRenterId(UUID renterId, Pageable pageable);

    Page<Deal> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Page<Deal> findAllByRenterIdAndStatus(UUID renterId, DealStatus status, Pageable pageable);

    Page<Deal> findAllByOwnerIdAndStatus(UUID ownerId, DealStatus status, Pageable pageable);

    @Query("""
        SELECT COUNT(d) > 0
        FROM Deal d
        WHERE d.itemId = :itemId
          AND d.status IN :statuses
          AND :startDate < d.endDate
          AND :endDate > d.startDate
        """)
    boolean existsDealConflict(
            @Param("itemId") UUID itemId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("statuses") Collection<DealStatus> statuses
    );

    @Query("""
        SELECT d FROM Deal d
        WHERE d.itemId = :itemId
          AND d.status = 'PENDING'
          AND d.id <> :excludeDealId
          AND :startDate < d.endDate
          AND :endDate > d.startDate
        """)
    List<Deal> findConflictingPendingDeals(
            @Param("itemId") UUID itemId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("excludeDealId") UUID excludeDealId
    );

    @Query("""
    SELECT COUNT(d) > 0
    FROM Deal d
    WHERE d.itemId = :itemId
      AND d.renterId = :renterId
      AND d.status IN :statuses
      AND :startDate < d.endDate
      AND :endDate > d.startDate
    """)
    boolean existsByRenterAndItemAndDateRange(
            @Param("itemId") UUID itemId,
            @Param("renterId") UUID renterId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("statuses") Collection<DealStatus> statuses
    );

    @Query("""
    SELECT d FROM Deal d
    WHERE d.status = :status
      AND d.endDate < :deadline
    """)
    List<Deal> findAllByStatusAndEndDateBefore(
            @Param("status") DealStatus status,
            @Param("deadline") OffsetDateTime deadline
    );
}