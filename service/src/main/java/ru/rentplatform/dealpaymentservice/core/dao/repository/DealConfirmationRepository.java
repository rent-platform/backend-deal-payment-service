package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealConfirmation;

import java.util.List;
import java.util.UUID;

public interface DealConfirmationRepository extends JpaRepository<DealConfirmation, UUID> {

    List<DealConfirmation> findAllByDeal_IdAndAction(UUID dealId, String action);

    long countByDeal_IdAndAction(UUID dealId, String action);

    boolean existsByDeal_IdAndUserIdAndAction(UUID dealId, UUID userId, String action);
}
