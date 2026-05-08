package ru.rentplatform.dealpaymentservice.core.service.implement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rentplatform.dealpaymentservice.api.dto.request.CancelDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.request.RejectDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.InvalidDealStatusException;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.util.DealResponseBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealStatusServiceImplTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private DealResponseBuilder dealResponseBuilder;

    @InjectMocks
    private DealStatusServiceImpl dealStatusService;

    private UUID dealId;

    private UUID ownerId;

    private UUID renterId;

    private Deal deal;

    @BeforeEach
    void setUp() {

        dealId = UUID.randomUUID();

        ownerId = UUID.randomUUID();

        renterId = UUID.randomUUID();

        deal = Deal.builder()
                .id(dealId)
                .ownerId(ownerId)
                .renterId(renterId)
                .status(DealStatus.PENDING)
                .build();

        lenient()
                .doNothing()
                .when(dealResponseBuilder)
                .saveStatusHistory(
                any(), any(), any(), any(), any(), any());
        lenient()
                .when(dealRepository.save(any(Deal.class)))
                .thenAnswer(inv ->
                        inv.getArgument(0));
    }

    @Test
    void confirmDeal_shouldConfirm_whenOwnerAndPending() {
        DealResponse response = DealResponse.builder().id(dealId).status("CONFIRMED").build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealRepository.findConflictingPendingDeals(any(), any(), any(), any())).thenReturn(List.of());
        when(dealResponseBuilder.buildDealResponse(any(Deal.class))).thenReturn(response);  // 🆕 any()

        DealResponse result = dealStatusService.confirmDeal(ownerId, dealId);

        assertNotNull(result);
        assertEquals(DealStatus.CONFIRMED, deal.getStatus());
    }

    @Test
    void confirmDeal_shouldThrow_whenNotOwner() {
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        assertThrows(DealAccessDeniedException.class, () ->
                dealStatusService.confirmDeal(renterId, dealId));
    }

    @Test
    void confirmDeal_shouldThrow_whenNotPending() {
        deal.setStatus(DealStatus.ACTIVE);
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        assertThrows(InvalidDealStatusException.class, () ->
                dealStatusService.confirmDeal(ownerId, dealId));
    }

    @Test
    void rejectDeal_shouldReject_whenOwnerAndPending() {
        RejectDealRequest request = new RejectDealRequest();
        request.setReason("Не подходит");

        DealResponse response = DealResponse.builder().id(dealId).status("REJECTED").build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealResponseBuilder.buildDealResponse(any(Deal.class))).thenReturn(response);  // 🆕 any()

        DealResponse result = dealStatusService.rejectDeal(ownerId, dealId, request);

        assertNotNull(result);
        assertEquals(DealStatus.REJECTED, deal.getStatus());
    }

    @Test
    void cancelDeal_shouldCancel_whenParticipant() {
        CancelDealRequest request = new CancelDealRequest();
        request.setReason("Передумал");

        DealResponse response = DealResponse.builder().id(dealId).status("CANCELLED").build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealResponseBuilder.buildDealResponse(any(Deal.class))).thenReturn(response);

        DealResponse result = dealStatusService.cancelDeal(renterId, dealId, request);

        assertNotNull(result);
        assertEquals(DealStatus.CANCELLED, deal.getStatus());
    }
}