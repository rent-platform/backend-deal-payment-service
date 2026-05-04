package ru.rentplatform.dealpaymentservice.core.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealChangeSource;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.service.PaymentService;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueDealService {

    private final DealRepository dealRepository;
    private final DealResponseBuilder dealResponseBuilder;
    private final PaymentService paymentService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkOverdueDeals() {
        OffsetDateTime deadline = OffsetDateTime.now().minusHours(3);

        var overdueDeals = dealRepository.findAllByStatusAndEndDateBefore(
                DealStatus.ACTIVE,
                deadline
        );

        for (Deal deal : overdueDeals) {
            log.warn("Deal {} is overdue (endDate: {}, deadline: {}). Penalty applied.",
                    deal.getId(), deal.getEndDate(), deadline);

            paymentService.processDealCompletion(deal, false);

            deal.setStatus(DealStatus.COMPLETED);
            deal.setRejectionReason("Overdue: renter did not return item on time (3+ hours)");
            deal.setUpdatedAt(OffsetDateTime.now());

            dealResponseBuilder.saveStatusHistory(
                    deal, DealStatus.ACTIVE, DealStatus.COMPLETED, null,
                    DealChangeSource.SYSTEM, "Deal auto-completed due to overdue"
            );

            log.info("Deal {} auto-completed with penalty", deal.getId());
        }
    }
}