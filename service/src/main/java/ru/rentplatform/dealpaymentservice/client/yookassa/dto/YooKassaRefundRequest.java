package ru.rentplatform.dealpaymentservice.client.yookassa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Запрос на возврат средств")
public class YooKassaRefundRequest {

    @JsonProperty("payment_id")
    @Schema(description = "ID платежа, по которому делается возврат",
            example = "2d5be9f2-000f-5000-8000-1a2b3c4d5e6f", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paymentId;

    @Schema(description = "Сумма возврата", requiredMode = Schema.RequiredMode.REQUIRED)
    private Amount amount;

    @Schema(description = "Описание возврата", example = "Deposit refund for deal <dealId>")
    private String description;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Сумма возврата")
    public static class Amount {

        @Schema(description = "Сумма в рублях с копейками", example = "5000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String value;

        @Schema(description = "Валюта", example = "RUB", requiredMode = Schema.RequiredMode.REQUIRED)
        private String currency;
    }
}
