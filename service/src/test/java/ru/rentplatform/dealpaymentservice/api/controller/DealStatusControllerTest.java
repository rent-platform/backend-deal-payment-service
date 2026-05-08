package ru.rentplatform.dealpaymentservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.dealpaymentservice.api.dto.response.DealResponse;
import ru.rentplatform.dealpaymentservice.core.service.DealStatusService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class DealStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DealStatusService dealStatusService;

    @Test
    void confirmDeal_shouldReturnConfirmed() throws Exception {
        UUID dealId = UUID.randomUUID();
        DealResponse response = DealResponse.builder().id(dealId).status("CONFIRMED").build();

        when(dealStatusService.confirmDeal(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/deals/" + dealId + "/confirm")
                        .with(jwt().jwt(j -> j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void rejectDeal_shouldReturnRejected() throws Exception {
        UUID dealId = UUID.randomUUID();
        DealResponse response = DealResponse.builder().id(dealId).status("REJECTED").build();

        when(dealStatusService.rejectDeal(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/deals/" + dealId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Не подходит\"}")
                        .with(jwt().jwt(j -> j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancelDeal_shouldReturnCancelled() throws Exception {
        UUID dealId = UUID.randomUUID();
        DealResponse response = DealResponse.builder().id(dealId).status("CANCELLED").build();

        when(dealStatusService.cancelDeal(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/deals/" + dealId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Передумал\"}")
                        .with(jwt().jwt(j -> j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
