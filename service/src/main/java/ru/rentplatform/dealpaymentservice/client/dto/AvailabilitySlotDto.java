package ru.rentplatform.dealpaymentservice.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Слот доступности товара из Catalog Service")
public record AvailabilitySlotDto(

        @JsonProperty("availableDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "Дата", example = "2026-05-15")
        LocalDate availableDate,

        @JsonProperty("isAvailable")
        @Schema(description = "Доступен ли товар в эту дату", example = "true")
        Boolean isAvailable

) {}