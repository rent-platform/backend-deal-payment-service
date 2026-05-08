package ru.rentplatform.dealpaymentservice.core.service.implement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateComplaintRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.ComplaintResponse;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Complaint;
import ru.rentplatform.dealpaymentservice.core.dao.entity.ComplaintStatus;
import ru.rentplatform.dealpaymentservice.core.dao.entity.ComplaintTargetType;
import ru.rentplatform.dealpaymentservice.core.dao.repository.ComplaintRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    @Test
    void createComplaint_shouldCreate_whenValid() {
        UUID authorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .targetType("USER").targetId(targetId).reason("Мошенничество").build();

        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID()).authorId(authorId)
                .targetType(ComplaintTargetType.USER).targetId(targetId)
                .reason("Мошенничество").status(ComplaintStatus.OPEN)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();

        when(complaintRepository.existsByAuthorIdAndTargetIdAndStatus(any(), any(), any())).thenReturn(false);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintResponse result = complaintService.createComplaint(authorId, request);

        assertNotNull(result);
        assertEquals("OPEN", result.getStatus());
        assertEquals("Мошенничество", result.getReason());
    }

    @Test
    void createComplaint_shouldThrow_whenAlreadyExists() {
        UUID authorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .targetType("USER").targetId(targetId).reason("Спам").build();

        when(complaintRepository.existsByAuthorIdAndTargetIdAndStatus(any(), any(), any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                complaintService.createComplaint(authorId, request));
    }
}
