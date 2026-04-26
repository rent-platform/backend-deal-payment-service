package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemDealInfoResponse {

    private UUID id;

    private UUID ownerId;

    private String status;

    private BigDecimal pricePerDay;

    private BigDecimal pricePerHour;

    private BigDecimal depositAmount;
}
