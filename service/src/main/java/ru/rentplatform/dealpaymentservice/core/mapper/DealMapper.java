package ru.rentplatform.dealpaymentservice.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.rentplatform.dealpaymentservice.api.dto.response.*;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealComment;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatusHistory;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Transaction;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DealMapper {

    @Mapping(target = "status", expression = "java(deal.getStatus().name())")
    @Mapping(target = "pricingMode", expression = "java(deal.getPricingMode().name())")
    DealShortResponse toDealShortResponse(Deal deal);

    @Mapping(target = "status", expression = "java(deal.getStatus().name())")
    @Mapping(target = "pricingMode", expression = "java(deal.getPricingMode().name())")
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "history", ignore = true)
    DealResponse toDealResponse(Deal deal);

    DealCommentResponse toDealCommentResponse(DealComment comment);

    @Mapping(target = "dealId", expression = "java(history.getDeal().getId())")
    @Mapping(target = "oldStatus", expression = "java(history.getOldStatus() != null ? history.getOldStatus().name() : null)")
    @Mapping(target = "newStatus", expression = "java(history.getNewStatus().name())")
    @Mapping(target = "changeSource", expression = "java(history.getChangeSource().name())")
    DealStatusHistoryResponse toDealStatusHistoryResponse(DealStatusHistory history);

    @Mapping(target = "dealId", expression = "java(transaction.getDeal().getId())")
    @Mapping(target = "type", expression = "java(transaction.getType().name())")
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    TransactionResponse toTransactionResponse(Transaction transaction);
}
