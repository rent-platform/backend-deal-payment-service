package ru.rentplatform.dealpaymentservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelDealRequest {

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
