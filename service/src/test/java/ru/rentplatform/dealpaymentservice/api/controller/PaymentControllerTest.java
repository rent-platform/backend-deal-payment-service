package ru.rentplatform.dealpaymentservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.dealpaymentservice.api.dto.response.PaymentConfirmationResponse;
import ru.rentplatform.dealpaymentservice.core.service.PaymentService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createPayment_shouldReturnConfirmationUrl() throws Exception {
        UUID dealId = UUID.randomUUID();
        PaymentConfirmationResponse response = PaymentConfirmationResponse.builder()
                .paymentId("payment_123")
                .confirmationUrl("https://yoomoney.ru/checkout/...")
                .status("pending")
                .build();

        when(paymentService.createPayment(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/deals/" + dealId + "/payment")
                        .with(jwt().jwt(j -> j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment_123"))
                .andExpect(jsonPath("$.status").value("pending"));
    }
}

