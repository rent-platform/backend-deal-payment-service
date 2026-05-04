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
@Schema(description = "Запрос на создание платежа в ЮKassa")
public class YooKassaPaymentRequest {

    @Schema(description = "Сумма платежа", requiredMode = Schema.RequiredMode.REQUIRED)
    private Amount amount;

    @JsonProperty("payment_method_data")
    @Schema(description = "Данные платёжного метода", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethodData paymentMethodData;

    @Schema(description = "Настройки подтверждения платежа", requiredMode = Schema.RequiredMode.REQUIRED)
    private Confirmation confirmation;

    @Schema(description = "Описание платежа", example = "Rent payment for deal <dealId>")
    private String description;

    @Schema(description = "true — сразу списать, false — холдировать (заморозить)", example = "false")
    private boolean capture;

    @Schema(description = "Метаданные (например, deal_id)")
    private Object metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Сумма платежа")
    public static class Amount {

        @Schema(description = "Сумма в рублях с копейками", example = "1500.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String value;

        @Schema(description = "Валюта", example = "RUB", requiredMode = Schema.RequiredMode.REQUIRED)
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Данные платёжного метода")
    public static class PaymentMethodData {

        @Schema(description = "Тип платёжного метода", example = "bank_card",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Настройки подтверждения платежа")
    public static class Confirmation {

        @Schema(description = "Тип подтверждения", example = "redirect",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String type;

        @JsonProperty("return_url")
        @Schema(description = "URL, куда вернётся пользователь после оплаты",
                example = "https://platform.ru/payment/return", requiredMode = Schema.RequiredMode.REQUIRED)
        private String returnUrl;
    }
}
