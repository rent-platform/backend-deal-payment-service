package ru.rentplatform.dealpaymentservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealCommentRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealCommentResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealComment;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealCommentRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;
import ru.rentplatform.dealpaymentservice.core.service.DealCommentService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealCommentServiceImpl implements DealCommentService {

    private final DealRepository dealRepository;
    private final DealCommentRepository dealCommentRepository;
    private final DealMapper dealMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DealCommentResponse> getDealComments(UUID currentUserId, UUID dealId) {
        Deal deal = getDeal(dealId);
        checkParticipant(currentUserId, deal);

        return dealCommentRepository.findAllByDeal_IdOrderByCreatedAtAsc(dealId)
                .stream()
                .map(dealMapper::toDealCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public DealCommentResponse addComment(UUID authorId, UUID dealId, CreateDealCommentRequest request) {
        Deal deal = getDeal(dealId);
        checkParticipant(authorId, deal);

        OffsetDateTime now = OffsetDateTime.now();

        DealComment comment = DealComment.builder()
                .id(UUID.randomUUID())
                .deal(deal)
                .authorId(authorId)
                .text(request.getText())
                .createdAt(now)
                .updatedAt(now)
                .build();

        DealComment savedComment = dealCommentRepository.save(comment);
        return dealMapper.toDealCommentResponse(savedComment);
    }

    private Deal getDeal(UUID dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));
    }

    private void checkParticipant(UUID userId, Deal deal) {
        if (!deal.getRenterId().equals(userId) && !deal.getOwnerId().equals(userId)) {
            throw new DealAccessDeniedException("Access denied");
        }
    }
}
