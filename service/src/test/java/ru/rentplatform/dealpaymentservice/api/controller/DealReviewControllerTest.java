package ru.rentplatform.dealpaymentservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealReviewResponse;
import ru.rentplatform.dealpaymentservice.core.service.DealReviewService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class DealReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DealReviewService dealReviewService;

    @Test
    void getUserReviews_shouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        DealReviewResponse review = DealReviewResponse.builder()
                .id(UUID.randomUUID()).dealId(UUID.randomUUID())
                .rating(5).text("Отлично!").build();

        when(dealReviewService.getUserReviews(any(), any()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/reviews/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void getItemReviews_shouldReturnList() throws Exception {
        UUID itemId = UUID.randomUUID();
        DealReviewResponse review = DealReviewResponse.builder()
                .id(UUID.randomUUID()).dealId(UUID.randomUUID())
                .rating(4).text("Хорошо").build();

        when(dealReviewService.getItemReviews(any(), any()))
                .thenReturn(new PageImpl<>(List.of(review)));

        mockMvc.perform(get("/api/reviews/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(4));
    }
}
