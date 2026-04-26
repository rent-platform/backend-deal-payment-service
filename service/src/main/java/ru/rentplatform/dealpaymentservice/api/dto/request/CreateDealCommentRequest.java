package ru.rentplatform.dealpaymentservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDealCommentRequest {

    @NotBlank
    @Size(max = 2000)
    private String text;
}
