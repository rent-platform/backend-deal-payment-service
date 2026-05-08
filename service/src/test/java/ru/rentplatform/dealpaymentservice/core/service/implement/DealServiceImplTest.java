package ru.rentplatform.dealpaymentservice.core.service.implement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.CatalogItemDealInfoResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.api.exception.DealTimeConflictException;
import ru.rentplatform.dealpaymentservice.client.catalog.CatalogClient;
import ru.rentplatform.dealpaymentservice.client.catalog.dto.AvailabilitySlotDto;
import ru.rentplatform.dealpaymentservice.core.dao.entity.Deal;
import ru.rentplatform.dealpaymentservice.core.dao.entity.DealStatus;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;
import ru.rentplatform.dealpaymentservice.core.util.DealResponseBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealServiceImplTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private DealMapper dealMapper;

    @Mock
    private DealResponseBuilder dealResponseBuilder;

    @InjectMocks
    private DealServiceImpl dealService;

    private UUID itemId;

    private UUID ownerId;

    private UUID renterId;

    private UUID dealId;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        renterId = UUID.randomUUID();
        dealId = UUID.randomUUID();
    }

    @Test
    void createDeal_shouldCreateDeal_whenValidRequest() {
        CreateDealRequest request = CreateDealRequest.builder()
                .itemId(itemId)
                .startDate(OffsetDateTime.now().plusDays(1))
                .endDate(OffsetDateTime.now().plusDays(3))
                .pricingMode("DAY")
                .build();

        CatalogItemDealInfoResponse itemInfo = CatalogItemDealInfoResponse.builder()
                .id(itemId)
                .ownerId(ownerId)
                .status("ACTIVE")
                .pricePerDay(new BigDecimal("500"))
                .depositAmount(new BigDecimal("1000"))
                .build();

        Deal deal = Deal.builder()
                .id(dealId).itemId(itemId).renterId(renterId).ownerId(ownerId)
                .status(DealStatus.PENDING).totalPrice(new BigDecimal("1000"))
                .build();

        DealResponse response = DealResponse.builder().id(dealId).status("PENDING").build();


        List<AvailabilitySlotDto> slots = createAvailableSlots(request.getStartDate(), request.getEndDate());

        when(catalogClient.getItemDealInfo(itemId)).thenReturn(itemInfo);
        when(catalogClient.getAvailability(eq(itemId), any(), any())).thenReturn(slots);
        when(dealRepository.existsDealConflict(any(), any(), any(), any())).thenReturn(false);
        when(dealRepository.existsByRenterAndItemAndDateRange(any(), any(), any(), any(), any())).thenReturn(false);
        when(dealRepository.save(any(Deal.class))).thenReturn(deal);
        when(dealResponseBuilder.buildDealResponse(any(Deal.class))).thenReturn(response);

        DealResponse result = dealService.createDeal(renterId, request);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(dealRepository).save(any(Deal.class));
    }

    @Test
    void createDeal_shouldThrow_whenOwnerRentsOwnItem() {
        CreateDealRequest request = CreateDealRequest.builder()
                .itemId(itemId)
                .startDate(OffsetDateTime.now().plusDays(1))
                .endDate(OffsetDateTime.now().plusDays(3))
                .pricingMode("DAY")
                .build();

        CatalogItemDealInfoResponse itemInfo = CatalogItemDealInfoResponse.builder()
                .id(itemId).ownerId(ownerId).status("ACTIVE").build();

        when(catalogClient.getItemDealInfo(itemId)).thenReturn(itemInfo);

        assertThrows(IllegalArgumentException.class, () ->
                dealService.createDeal(ownerId, request));
    }

    @Test
    void createDeal_shouldThrow_whenItemNotActive() {
        CreateDealRequest request = CreateDealRequest.builder()
                .itemId(itemId)
                .startDate(OffsetDateTime.now().plusDays(1))
                .endDate(OffsetDateTime.now().plusDays(3))
                .pricingMode("DAY")
                .build();

        CatalogItemDealInfoResponse itemInfo = CatalogItemDealInfoResponse.builder()
                .id(itemId).ownerId(ownerId).status("DRAFT").build();

        when(catalogClient.getItemDealInfo(itemId)).thenReturn(itemInfo);

        assertThrows(IllegalArgumentException.class, () ->
                dealService.createDeal(renterId, request));
    }

    @Test
    void createDeal_shouldThrow_whenTimeConflict() {
        CreateDealRequest request = CreateDealRequest.builder()
                .itemId(itemId)
                .startDate(OffsetDateTime.now().plusDays(1))
                .endDate(OffsetDateTime.now().plusDays(3))
                .pricingMode("DAY")
                .build();

        CatalogItemDealInfoResponse itemInfo = CatalogItemDealInfoResponse.builder()
                .id(itemId).ownerId(ownerId).status("ACTIVE")
                .pricePerDay(new BigDecimal("500")).depositAmount(BigDecimal.ZERO).build();

        List<AvailabilitySlotDto> slots = createAvailableSlots(request.getStartDate(), request.getEndDate());

        when(catalogClient.getItemDealInfo(itemId)).thenReturn(itemInfo);
        when(catalogClient.getAvailability(eq(itemId), any(), any())).thenReturn(slots);
        when(dealRepository.existsDealConflict(any(), any(), any(), any())).thenReturn(true);

        assertThrows(DealTimeConflictException.class, () ->
                dealService.createDeal(renterId, request));
    }

    @Test
    void getDealById_shouldReturnDeal_whenParticipant() {
        Deal deal = Deal.builder()
                .id(dealId).renterId(renterId).ownerId(ownerId)
                .status(DealStatus.PENDING).build();

        DealResponse response = DealResponse.builder().id(dealId).status("PENDING").build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(dealResponseBuilder.buildDealResponse(deal)).thenReturn(response);

        DealResponse result = dealService.getDealById(renterId, dealId);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getDealById_shouldThrow_whenNotParticipant() {
        Deal deal = Deal.builder()
                .id(dealId).renterId(renterId).ownerId(ownerId).build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        assertThrows(DealAccessDeniedException.class, () ->
                dealService.getDealById(UUID.randomUUID(), dealId));
    }

    @Test
    void getDealById_shouldThrow_whenNotFound() {
        when(dealRepository.findById(dealId)).thenReturn(Optional.empty());

        assertThrows(DealNotFoundException.class, () ->
                dealService.getDealById(renterId, dealId));
    }

    private List<AvailabilitySlotDto> createAvailableSlots(OffsetDateTime start, OffsetDateTime end) {
        List<AvailabilitySlotDto> slots = new ArrayList<>();
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            slots.add(new AvailabilitySlotDto(d, true));
        }
        return slots;
    }
}

