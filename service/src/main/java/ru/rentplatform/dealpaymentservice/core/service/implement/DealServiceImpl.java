package ru.rentplatform.dealpaymentservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.dealpaymentservice.api.dto.request.CreateDealRequest;
import ru.rentplatform.dealpaymentservice.api.dto.response.*;
import ru.rentplatform.dealpaymentservice.api.exception.DealAccessDeniedException;
import ru.rentplatform.dealpaymentservice.api.exception.DealNotFoundException;
import ru.rentplatform.dealpaymentservice.api.exception.DealTimeConflictException;
import ru.rentplatform.dealpaymentservice.client.catalog.CatalogClient;
import ru.rentplatform.dealpaymentservice.core.dao.entity.*;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealCommentRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealStatusHistoryRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;
import ru.rentplatform.dealpaymentservice.core.service.DealService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final DealCommentRepository dealCommentRepository;
    private final DealStatusHistoryRepository dealStatusHistoryRepository;
    private final CatalogClient catalogClient;
    private final DealMapper dealMapper;

    @Override
    @Transactional
    public DealResponse createDeal(UUID renterId, CreateDealRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        PricingMode pricingMode = parsePricingMode(request.getPricingMode());

        CatalogItemDealInfoResponse itemInfo = catalogClient.getItemDealInfo(request.getItemId());

        if (itemInfo == null) {
            throw new DealNotFoundException("Item not found");
        }

        if (!"ACTIVE".equals(itemInfo.getStatus())) {
            throw new IllegalArgumentException("Item is not available for rent");
        }

        if (itemInfo.getOwnerId().equals(renterId)) {
            throw new IllegalArgumentException("Owner cannot rent own item");
        }

        checkConflicts(request.getItemId(), request.getStartDate(), request.getEndDate());

        BigDecimal totalPrice = calculateTotalPrice(
                pricingMode,
                request.getStartDate(),
                request.getEndDate(),
                itemInfo
        );

        OffsetDateTime now = OffsetDateTime.now();

        Deal deal = Deal.builder()
                .id(UUID.randomUUID())
                .itemId(itemInfo.getId())
                .renterId(renterId)
                .ownerId(itemInfo.getOwnerId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .pricingMode(pricingMode)
                .pricePerDaySnapshot(pricingMode == PricingMode.DAY ? itemInfo.getPricePerDay() : null)
                .pricePerHourSnapshot(pricingMode == PricingMode.HOUR ? itemInfo.getPricePerHour() : null)
                .totalPrice(totalPrice)
                .depositAmount(itemInfo.getDepositAmount() != null ? itemInfo.getDepositAmount() : BigDecimal.ZERO)
                .status(DealStatus.PENDING)
                .rejectionReason(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Deal savedDeal = dealRepository.save(deal);

        saveStatusHistory(
                savedDeal,
                null,
                DealStatus.PENDING,
                renterId,
                DealChangeSource.USER,
                "Deal created"
        );

        return buildDealResponse(savedDeal);
    }

    @Override
    @Transactional(readOnly = true)
    public DealResponse getDealById(UUID currentUserId, UUID dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException("Deal not found"));

        if (!deal.getRenterId().equals(currentUserId) && !deal.getOwnerId().equals(currentUserId)) {
            throw new DealAccessDeniedException("Access denied");
        }

        return buildDealResponse(deal);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DealShortResponse> getMyRenterDeals(UUID renterId, DealStatus status, Pageable pageable) {
        Page<Deal> deals = status == null
                ? dealRepository.findAllByRenterId(renterId, pageable)
                : dealRepository.findAllByRenterIdAndStatus(renterId, status, pageable);

        return deals.map(dealMapper::toDealShortResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DealShortResponse> getMyOwnerDeals(UUID ownerId, DealStatus status, Pageable pageable) {
        Page<Deal> deals = status == null
                ? dealRepository.findAllByOwnerId(ownerId, pageable)
                : dealRepository.findAllByOwnerIdAndStatus(ownerId, status, pageable);

        return deals.map(dealMapper::toDealShortResponse);
    }

    private void validateDates(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (startDate.isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }

    private PricingMode parsePricingMode(String value) {
        try {
            return PricingMode.valueOf(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid pricing mode");
        }
    }

    private void checkConflicts(UUID itemId, OffsetDateTime startDate, OffsetDateTime endDate) {
        boolean hasConflict = dealRepository.existsDealConflict(
                itemId,
                startDate,
                endDate,
                List.of(DealStatus.PENDING, DealStatus.CONFIRMED, DealStatus.ACTIVE)
        );

        if (hasConflict) {
            throw new DealTimeConflictException("Item is already booked for selected period");
        }
    }

    private BigDecimal calculateTotalPrice(PricingMode pricingMode,
                                           OffsetDateTime startDate,
                                           OffsetDateTime endDate,
                                           CatalogItemDealInfoResponse itemInfo) {
        if (pricingMode == PricingMode.DAY) {
            if (itemInfo.getPricePerDay() == null) {
                throw new IllegalArgumentException("Daily price is not available for this item");
            }

            long hours = Duration.between(startDate, endDate).toHours();
            long days = (long) Math.ceil(hours / 24.0);

            if (days <= 0) {
                days = 1;
            }

            return itemInfo.getPricePerDay()
                    .multiply(BigDecimal.valueOf(days))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (pricingMode == PricingMode.HOUR) {
            if (itemInfo.getPricePerHour() == null) {
                throw new IllegalArgumentException("Hourly price is not available for this item");
            }

            long minutes = Duration.between(startDate, endDate).toMinutes();
            long hours = (long) Math.ceil(minutes / 60.0);

            if (hours <= 0) {
                hours = 1;
            }

            return itemInfo.getPricePerHour()
                    .multiply(BigDecimal.valueOf(hours))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        throw new IllegalArgumentException("Invalid pricing mode");
    }

    private void saveStatusHistory(Deal deal,
                                   DealStatus oldStatus,
                                   DealStatus newStatus,
                                   UUID changedBy,
                                   DealChangeSource changeSource,
                                   String comment) {
        DealStatusHistory history = DealStatusHistory.builder()
                .id(UUID.randomUUID())
                .deal(deal)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .changeSource(changeSource)
                .comment(comment)
                .changedAt(OffsetDateTime.now())
                .build();

        dealStatusHistoryRepository.save(history);
    }

    private DealResponse buildDealResponse(Deal deal) {
        List<DealCommentResponse> comments = dealCommentRepository.findAllByDeal_IdOrderByCreatedAtAsc(deal.getId())
                .stream()
                .map(dealMapper::toDealCommentResponse)
                .toList();

        List<DealStatusHistoryResponse> history = dealStatusHistoryRepository.findAllByDeal_IdOrderByChangedAtAsc(deal.getId())
                .stream()
                .map(dealMapper::toDealStatusHistoryResponse)
                .toList();

        DealResponse response = dealMapper.toDealResponse(deal);
        response.setComments(comments);
        response.setHistory(history);

        return response;
    }
}
