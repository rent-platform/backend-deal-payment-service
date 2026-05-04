package ru.rentplatform.dealpaymentservice.client.yookassa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Запрос на списание (каптуру) замороженного платежа")
public class YooKassaCaptureRequest {

    @Schema(description = "Сумма к списанию (может быть меньше замороженной)")
    private Amount amount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Сумма списания")
    public static class Amount {

        @Schema(description = "Сумма в рублях с копейками", example = "1000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String value;

        @Schema(description = "Валюта", example = "RUB", requiredMode = Schema.RequiredMode.REQUIRED)
        private String currency;
    }
}
