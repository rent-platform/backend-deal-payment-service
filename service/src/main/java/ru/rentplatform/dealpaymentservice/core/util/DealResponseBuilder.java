package ru.rentplatform.dealpaymentservice.core.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealStatusHistoryResponse;
import ru.rentplatform.dealpaymentservice.core.dao.entity.*;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealStatusHistoryRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DealResponseBuilder {

    private final DealStatusHistoryRepository dealStatusHistoryRepository;
    private final DealMapper dealMapper;

    public void saveStatusHistory(Deal deal,
                                  DealStatus oldStatus,
                                  DealStatus newStatus,
                                  UUID changedBy,
                                  DealChangeSource changeSource,
                                  String comment) {
        DealStatusHistory history = DealStatusHistory.builder()
                .deal(deal)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .changeSource(changeSource)
                .comment(comment)
                .changedAt(OffsetDateTime.now())
                .build();

        dealStatusHistoryRepository.save(history);
    }

    public DealResponse buildDealResponse(Deal deal) {
        List<DealStatusHistoryResponse> history = dealStatusHistoryRepository
                .findAllByDeal_IdOrderByChangedAtAsc(deal.getId())
                .stream()
                .map(dealMapper::toDealStatusHistoryResponse)
                .toList();

        DealResponse response = dealMapper.toDealResponse(deal);
        response.setHistory(history);

        return response;
    }
}