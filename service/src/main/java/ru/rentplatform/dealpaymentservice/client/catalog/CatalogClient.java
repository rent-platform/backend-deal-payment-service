package ru.rentplatform.dealpaymentservice.client.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.rentplatform.dealpaymentservice.api.dto.response.CatalogItemDealInfoResponse;

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
}