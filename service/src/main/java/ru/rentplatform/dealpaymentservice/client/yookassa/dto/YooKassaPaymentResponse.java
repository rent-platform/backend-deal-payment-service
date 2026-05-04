package ru.rentplatform.dealpaymentservice.client.yookassa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ответ от ЮKassa после создания платежа")
public class YooKassaPaymentResponse {

    @Schema(description = "ID платежа в ЮKassa", example = "2d5be9f2-000f-5000-8000-1a2b3c4d5e6f")
    private String id;

    @Schema(description = "Статус платежа", example = "pending")
    private String status;

    @Schema(description = "Сумма платежа")
    private Map<String, Object> amount;

    @JsonProperty("payment_method")
    @Schema(description = "Данные платёжного метода")
    private Map<String, Object> paymentMethod;

    @Schema(description = "Данные для подтверждения (confirmation_url для редиректа)")
    private Map<String, Object> confirmation;

    @JsonProperty("created_at")
    @Schema(description = "Дата создания платежа")
    private String createdAt;

    @JsonProperty("expires_at")
    @Schema(description = "Дата истечения срока платежа")
    private String expiresAt;

    @Schema(description = "Метаданные, переданные при создании")
    private Object metadata;

    @JsonProperty("cancellation_details")
    @Schema(description = "Детали отмены (если платёж отменён)")
    private Map<String, Object> cancellationDetails;
}