package ru.rentplatform.dealpaymentservice.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на обработку жалобы")
public class HandleComplaintRequest {

    @NotBlank
    @Schema(description = "Решение: RESOLVED или DISMISSED", example = "RESOLVED")
    private String status;

    @Schema(description = "Комментарий решения", example = "Пользователь заблокирован")
    private String resolution;
}
