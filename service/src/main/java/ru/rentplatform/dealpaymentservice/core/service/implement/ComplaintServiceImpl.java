package ru.rentplatform.dealpaymentservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateComplaintRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.ComplaintResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.client.audit.AuditClient;
import ru.rentplatform.dealpaymentservice.core.dao.entity.*;
import ru.rentplatform.dealpaymentservice.core.dao.repository.ComplaintRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealReviewRepository;
import ru.rentplatform.dealpaymentservice.core.service.ComplaintService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final DealReviewRepository dealReviewRepository;
    private final AuditClient auditClient;

    @Override
    @Transactional
    public ComplaintResponse createComplaint(UUID authorId, CreateComplaintRequest request) {
        boolean alreadyExists = complaintRepository.existsByAuthorIdAndTargetIdAndStatus(
                authorId, request.getTargetId(), ComplaintStatus.OPEN);

        if (alreadyExists) {
            throw new IllegalArgumentException("You already have an open complaint for this target");
        }

        Complaint complaint = Complaint.builder()
                .authorId(authorId)
                .targetType(ComplaintTargetType.valueOf(request.getTargetType()))
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .status(ComplaintStatus.OPEN)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Complaint saved = complaintRepository.save(complaint);

        auditClient.sendLog("deal-payment-service", authorId, "user",
                "CREATE_COMPLAINT", "COMPLAINT", saved.getId().toString(),
                "{\"reason\": \"" + request.getReason() + "\"}");

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintResponse> getComplaints(String statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return complaintRepository.findAllByStatus(ComplaintStatus.valueOf(statusFilter), pageable)
                    .map(this::toResponse);
        }
        return complaintRepository.findAllByStatusIn(
                        List.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ComplaintResponse handleComplaint(UUID complaintId, UUID moderatorId, String status, String resolution) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new DealNotFoundException("Complaint not found"));

        if (complaint.getStatus() != ComplaintStatus.OPEN
                && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Complaint is already handled");
        }

        complaint.setStatus(ComplaintStatus.valueOf(status));
        complaint.setHandledBy(moderatorId);
        complaint.setResolution(resolution);
        complaint.setUpdatedAt(OffsetDateTime.now());

        if (complaint.getTargetType() == ComplaintTargetType.REVIEW
                && "RESOLVED".equals(status)) {
            dealReviewRepository.deleteById(complaint.getTargetId());
        }

        complaintRepository.save(complaint);

        auditClient.sendLog("deal-payment-service", moderatorId, "moderator",
                "RESOLVE_COMPLAINT", "COMPLAINT", complaintId.toString(),
                "{\"status\": \"" + status + "\"}");

        return toResponse(complaint);
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .authorId(complaint.getAuthorId())
                .targetType(complaint.getTargetType().name())
                .targetId(complaint.getTargetId())
                .reason(complaint.getReason())
                .status(complaint.getStatus().name())
                .handledBy(complaint.getHandledBy())
                .resolution(complaint.getResolution())
                .createdAt(complaint.getCreatedAt())
                .build();
    }
}
