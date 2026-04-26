package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealCommentRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealCommentResponse;
import ru.rentplatform.dealpaymentservice.core.service.DealCommentService;

import java.util.List;
import java.util.UUID;

import static ru.rentplatform.dealpaymentservice.api.ApiPaths.DEALS;

@RestController
@RequestMapping(DEALS + "/{dealId}/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DealCommentController {

    private final DealCommentService dealCommentService;

    @GetMapping
    public List<DealCommentResponse> getDealComments(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID dealId) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return dealCommentService.getDealComments(currentUserId, dealId);
    }

    @PostMapping
    public DealCommentResponse addComment(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID dealId,
                                          @Valid @RequestBody CreateDealCommentRequest request) {
        UUID authorId = UUID.fromString(jwt.getSubject());
        return dealCommentService.addComment(authorId, dealId, request);
    }
}
