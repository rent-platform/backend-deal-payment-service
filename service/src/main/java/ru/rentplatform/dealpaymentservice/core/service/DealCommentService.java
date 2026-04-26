package ru.rentplatform.dealpaymentservice.core.service;

import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealCommentRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealCommentResponse;

import java.util.List;
import java.util.UUID;

public interface DealCommentService {

    List<DealCommentResponse> getDealComments(UUID currentUserId, UUID dealId);

    DealCommentResponse addComment(UUID authorId, UUID dealId, CreateDealCommentRequest request);
}
