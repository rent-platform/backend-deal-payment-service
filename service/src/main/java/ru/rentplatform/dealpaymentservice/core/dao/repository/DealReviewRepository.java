package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealReview;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealReviewType;

import java.util.List;
import java.util.UUID;

public interface DealReviewRepository extends JpaRepository<DealReview, UUID> {

    boolean existsByDeal_IdAndReviewerId(UUID dealId, UUID reviewerId);

    List<DealReview> findAllByDeal_IdOrderByCreatedAtAsc(UUID dealId);

    Page<DealReview> findAllByReviewedUserId(UUID reviewedUserId, Pageable pageable);

    Page<DealReview> findAllByItemIdAndReviewType(UUID itemId, DealReviewType reviewType, Pageable pageable);
}
