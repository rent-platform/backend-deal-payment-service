package ru.rentplatform.dealpaymentservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByDeal_Id(UUID dealId);
}
