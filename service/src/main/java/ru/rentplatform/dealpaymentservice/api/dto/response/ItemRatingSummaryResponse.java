package ru.rentplatform.dealpaymentservice.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Сводка рейтинга товара")
public class ItemRatingSummaryResponse {

    @Schema(description = "Средний рейтинг товара", example = "4.8")
    private Double averageRating;

    @Schema(description = "Общее количество отзывов", example = "12")
    private long totalReviews;
}
