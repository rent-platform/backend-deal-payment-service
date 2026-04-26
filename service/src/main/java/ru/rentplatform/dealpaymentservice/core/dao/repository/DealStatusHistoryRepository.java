package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatusHistory;

import java.util.List;
import java.util.UUID;

public interface DealStatusHistoryRepository extends JpaRepository<DealStatusHistory, UUID> {

    List<DealStatusHistory> findAllByDeal_IdOrderByChangedAtAsc(UUID dealId);
}
