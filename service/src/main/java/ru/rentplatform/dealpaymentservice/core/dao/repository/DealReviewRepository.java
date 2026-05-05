package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealReview;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealReviewType;

import java.util.List;
import java.util.UUID;

public interface DealReviewRepository extends JpaRepository<DealReview, UUID> {

    boolean existsByDeal_IdAndReviewerId(UUID dealId, UUID reviewerId);

    List<DealReview> findAllByDeal_IdOrderByCreatedAtAsc(UUID dealId);

    Page<DealReview> findAllByReviewedUserId(UUID reviewedUserId, Pageable pageable);

    Page<DealReview> findAllByItemIdAndReviewType(UUID itemId, DealReviewType reviewType, Pageable pageable);

    @Query("""
    SELECT AVG(r.rating) FROM DealReview r
    WHERE r.reviewedUserId = :userId
      AND r.reviewType = 'RENTER_TO_OWNER'
    """)
    Double getOwnerRatingByUserId(@Param("userId") UUID userId);

    @Query("""
    SELECT COUNT(r) FROM DealReview r
    WHERE r.reviewedUserId = :userId
      AND r.reviewType = 'RENTER_TO_OWNER'
    """)
    long countOwnerReviewsByUserId(@Param("userId") UUID userId);

    @Query("""
    SELECT AVG(r.rating) FROM DealReview r
    WHERE r.reviewedUserId = :userId
      AND r.reviewType = 'OWNER_TO_RENTER'
    """)
    Double getRenterRatingByUserId(@Param("userId") UUID userId);

    @Query("""
    SELECT COUNT(r) FROM DealReview r
    WHERE r.reviewedUserId = :userId
      AND r.reviewType = 'OWNER_TO_RENTER'
    """)
    long countRenterReviewsByUserId(@Param("userId") UUID userId);

    @Query("""
    SELECT AVG(r.rating) FROM DealReview r
    WHERE r.itemId = :itemId
      AND r.reviewType = 'RENTER_TO_OWNER'
    """)
    Double getAverageRatingByItemId(@Param("itemId") UUID itemId);

    @Query("""
    SELECT COUNT(r) FROM DealReview r
    WHERE r.itemId = :itemId
      AND r.reviewType = 'RENTER_TO_OWNER'
    """)
    long countByItemId(@Param("itemId") UUID itemId);
}
