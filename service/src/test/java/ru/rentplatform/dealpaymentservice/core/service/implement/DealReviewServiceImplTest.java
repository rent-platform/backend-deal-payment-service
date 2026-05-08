package ru.rentplatform.dealpaymentservice.core.service.implement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rentplatform.dealpaymentservice.api.dto.response.ItemRatingSummaryResponse;
import ru.rentplatform.dealpaymentservice.api.dto.response.UserRatingSummaryResponse;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealRepository;
import ru.rentplatform.dealpaymentservice.core.dao.repository.DealReviewRepository;
import ru.rentplatform.dealpaymentservice.core.mapper.DealMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealReviewServiceImplTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private DealReviewRepository dealReviewRepository;

    @Mock
    private DealMapper dealMapper;

    @InjectMocks
    private DealReviewServiceImpl dealReviewService;

    @Test
    void getUserRatingSummary_shouldReturnRating() {
        UUID userId = UUID.randomUUID();
        when(dealReviewRepository.getOwnerRatingByUserId(userId)).thenReturn(5.0);
        when(dealReviewRepository.getRenterRatingByUserId(userId)).thenReturn(4.5);
        when(dealReviewRepository.countOwnerReviewsByUserId(userId)).thenReturn(3L);
        when(dealReviewRepository.countRenterReviewsByUserId(userId)).thenReturn(2L);

        UserRatingSummaryResponse result = dealReviewService.getUserRatingSummary(userId);

        assertNotNull(result);
        assertEquals(4.8, result.getOverallRating(), 0.1);
        assertEquals(5, result.getTotalReviews());
        assertEquals(5.0, result.getOwnerRating(), 0.1);
        assertEquals(4.5, result.getRenterRating(), 0.1);
    }

    @Test
    void getItemRatingSummary_shouldReturnRating() {
        UUID itemId = UUID.randomUUID();
        when(dealReviewRepository.getAverageRatingByItemId(itemId)).thenReturn(4.5);
        when(dealReviewRepository.countByItemId(itemId)).thenReturn(10L);

        ItemRatingSummaryResponse result = dealReviewService.getItemRatingSummary(itemId);

        assertNotNull(result);
        assertEquals(4.5, result.getAverageRating(), 0.1);
        assertEquals(10, result.getTotalReviews());
    }
}
