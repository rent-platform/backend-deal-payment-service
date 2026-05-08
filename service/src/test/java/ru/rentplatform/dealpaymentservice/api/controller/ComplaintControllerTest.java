package ru.rentplatform.dealpaymentservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.dealpaymentservice.api.dto.response.ComplaintResponse;
import ru.rentplatform.dealpaymentservice.core.service.ComplaintService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintService complaintService;

    @Test
    void createComplaint_shouldReturnComplaint() throws Exception {
        UUID complaintId = UUID.randomUUID();
        ComplaintResponse response = ComplaintResponse.builder()
                .id(complaintId).authorId(UUID.randomUUID())
                .targetType("USER").targetId(UUID.randomUUID())
                .reason("Мошенничество").status("OPEN")
                .createdAt(OffsetDateTime.now()).build();

        when(complaintService.createComplaint(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "targetType": "USER",
                            "targetId": "%s",
                            "reason": "Мошенничество"
                        }
                        """.formatted(UUID.randomUUID()))
                        .with(jwt().jwt(j ->
                                j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }
}
