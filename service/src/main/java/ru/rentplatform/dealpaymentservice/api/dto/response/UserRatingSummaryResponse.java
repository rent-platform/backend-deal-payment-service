package ru.rentplatform.dealpaymentservice.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Сводка рейтинга")
public class UserRatingSummaryResponse {

    @Schema(description = "Общий рейтинг (средний между арендатором и арендодателем)", example = "4.5")
    private Double overallRating;

    @Schema(description = "Общее количество отзывов", example = "8")
    private long totalReviews;

    @Schema(description = "Рейтинг как арендодатель", example = "4.8")
    private Double ownerRating;

    @Schema(description = "Количество отзывов как арендодатель", example = "5")
    private long ownerReviews;

    @Schema(description = "Рейтинг как арендатор", example = "4.0")
    private Double renterRating;

    @Schema(description = "Количество отзывов как арендатор", example = "3")
    private long renterReviews;
}
