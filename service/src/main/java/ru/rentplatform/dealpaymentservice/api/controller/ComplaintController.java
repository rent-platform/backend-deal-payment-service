package ru.rentplatform.dealpaymentservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateComplaintRequest;
import ru.rentplatform.dealpaymentservice.api.dto.request.HandleComplaintRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.ComplaintResponse;
import ru.rentplatform.dealpaymentservice.core.service.ComplaintService;

import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Жалобы", description = "Создание и обработка жалоб на пользователей и объявления")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @Operation(summary = "Подать жалобу", description = "Авторизованный пользователь подаёт жалобу " +
            "на другого пользователя или объявление")
    public ComplaintResponse createComplaint(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateComplaintRequest request) {
        UUID authorId = UUID.fromString(jwt.getSubject());
        return complaintService.createComplaint(authorId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('super_admin', 'admin', 'moderator')")
    @Operation(summary = "Список жалоб", description = "Модератор, админ или супер-админ просматривает " +
            "список жалоб. По умолчанию OPEN и IN_PROGRESS")
    public Page<ComplaintResponse> getComplaints(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return complaintService.getComplaints(status, pageable);
    }

    @PutMapping("/{complaintId}/handle")
    @PreAuthorize("hasAnyRole('super_admin', 'admin', 'moderator')")
    @Operation(summary = "Обработать жалобу", description = "Модератор принимает решение по жалобе: " +
            "RESOLVED или DISMISSED")
    public ComplaintResponse handleComplaint(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID complaintId,
                                             @Valid @RequestBody HandleComplaintRequest request) {
        UUID moderatorId = UUID.fromString(jwt.getSubject());
        return complaintService.handleComplaint(complaintId, moderatorId, request.getStatus(), request.getResolution());
    }
}