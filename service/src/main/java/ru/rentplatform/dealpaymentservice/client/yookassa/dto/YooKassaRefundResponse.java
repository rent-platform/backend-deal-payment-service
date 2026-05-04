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
@Schema(description = "Ответ от ЮKassa после создания возврата")
public class YooKassaRefundResponse {

    @Schema(description = "ID возврата в ЮKassa", example = "2d5be9f2-000f-5000-8000-1e5f6g7h8i9j")
    private String id;

    @JsonProperty("payment_id")
    @Schema(description = "ID исходного платежа", example = "2d5be9f2-000f-5000-8000-1a2b3c4d5e6f")
    private String paymentId;

    @Schema(description = "Статус возврата", example = "succeeded")
    private String status;

    @Schema(description = "Сумма возврата")
    private Map<String, Object> amount;

    @JsonProperty("created_at")
    @Schema(description = "Дата создания возврата")
    private String createdAt;

    @Schema(description = "Описание возврата")
    private String description;
}