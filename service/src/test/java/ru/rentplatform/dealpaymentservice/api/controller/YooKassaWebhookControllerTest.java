package ru.rentplatform.dealpaymentservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.dealpaymentservice.core.service.PaymentService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class YooKassaWebhookControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldHandlePaymentSucceeded() throws Exception {
        mockMvc.perform(post("/api/webhooks/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "event": "payment.succeeded",
                            "object": {
                                "id": "payment_123",
                                "status": "succeeded"
                            }
                        }
                        """))
                .andExpect(status().isOk());

        verify(paymentService).handlePaymentSuccess(eq("payment_123"));
    }

    @Test
    void shouldIgnoreOtherEvents() throws Exception {
        mockMvc.perform(post("/api/webhooks/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "event": "payment.canceled",
                            "object": {
                                "id": "payment_456",
                                "status": "canceled"
                            }
                        }
                        """))
                .andExpect(status().isOk());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReturnOk_whenNoObject() throws Exception {
        mockMvc.perform(post("/api/webhooks/yookassa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "event": "payment.succeeded"
                        }
                        """))
                .andExpect(status().isOk());

        verifyNoInteractions(paymentService);
    }
}