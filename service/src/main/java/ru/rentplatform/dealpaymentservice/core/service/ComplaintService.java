package ru.rentplatform.dealpaymentservice.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateComplaintRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.ComplaintResponse;

import java.util.UUID;

public interface ComplaintService {

    ComplaintResponse createComplaint(UUID authorId, CreateComplaintRequest request);

    Page<ComplaintResponse> getComplaints(String statusFilter, Pageable pageable);

    ComplaintResponse handleComplaint(UUID complaintId, UUID moderatorId, String status, String resolution);
}
