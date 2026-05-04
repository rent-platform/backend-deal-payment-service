package ru.rentplatform.dealpaymentservice.client.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.rentplatform.dealpaymentservice.api.dto.response.CatalogItemDealInfoResponse;
import ru.rentplatform.dealpaymentservice.client.catalog.dto.AvailabilitySlotDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final RestClient catalogServiceRestClient;

    public CatalogItemDealInfoResponse getItemDealInfo(UUID itemId) {
        return catalogServiceRestClient.get()
                .uri("/api/catalog/items/{itemId}/deal-info", itemId)
                .retrieve()
                .body(CatalogItemDealInfoResponse.class);
    }

    public List<AvailabilitySlotDto> getAvailability(UUID itemId, LocalDate startDate, LocalDate endDate) {
        return catalogServiceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/catalog/items/{itemId}/availability")
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .build(itemId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}