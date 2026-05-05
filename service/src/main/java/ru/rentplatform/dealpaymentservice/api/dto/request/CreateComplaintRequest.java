package ru.rentplatform.dealpaymentservice.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание жалобы")
public class CreateComplaintRequest {

    @NotNull
    @Schema(description = "Тип жалобы: USER или ITEM", example = "USER")
    private String targetType;

    @NotNull
    @Schema(description = "ID пользователя или объявления")
    private UUID targetId;

    @NotBlank
    @Schema(description = "Причина жалобы", example = "Мошенничество")
    private String reason;
}
