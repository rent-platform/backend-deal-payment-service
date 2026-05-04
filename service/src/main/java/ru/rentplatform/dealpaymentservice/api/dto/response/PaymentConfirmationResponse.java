package ru.rentplatform.dealpaymentservice.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ со ссылкой на оплату для редиректа пользователя в ЮKassa")
public class PaymentConfirmationResponse {

    @Schema(description = "ID платежа в ЮKassa", example = "2d5be9f2-000f-5000-8000-1a2b3c4d5e6f")
    private String paymentId;

    @Schema(description = "Ссылка на платёжную форму ЮKassa, куда нужно редиректить пользователя",
            example = "https://yoomoney.ru/checkout/payments/v2/contract?orderId=...")
    private String confirmationUrl;

    @Schema(description = "Статус платежа", example = "pending")
    private String status;
}
