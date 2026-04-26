package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealComment;

import java.util.List;
import java.util.UUID;

public interface DealCommentRepository extends JpaRepository<DealComment, UUID> {

    List<DealComment> findAllByDeal_IdOrderByCreatedAtAsc(UUID dealId);
}
