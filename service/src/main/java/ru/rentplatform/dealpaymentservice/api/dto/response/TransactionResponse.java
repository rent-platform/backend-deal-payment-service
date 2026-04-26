package ru.rentplatform.dealpaymentservice.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;

    private UUID dealId;

    private String type;

    private BigDecimal amount;

    private String status;

    private String yookassaPaymentId;

    private String yookassaPaymentMethodId;

    private Map<String, Object> gatewayResponse;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
