package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Complaint;
import ru.rentplatform.dealpaymentservice.core.dao.entity.ComplaintStatus;

import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    Page<Complaint> findAllByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findAllByStatusIn(java.util.List<ComplaintStatus> statuses, Pageable pageable);

    boolean existsByAuthorIdAndTargetIdAndStatus(UUID authorId, UUID targetId, ComplaintStatus status);
}
