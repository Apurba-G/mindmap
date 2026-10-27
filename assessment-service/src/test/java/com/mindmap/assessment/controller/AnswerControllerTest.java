package com.mindmap.assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.assessment.dto.ProgressDto;
import com.mindmap.assessment.dto.SafetyAlert;
import com.mindmap.assessment.dto.SubmitAnswerRequest;
import com.mindmap.assessment.dto.SubmitAnswerResponse;
import com.mindmap.assessment.security.JwtAuthenticationFilter;
import com.mindmap.assessment.security.UserPrincipal;
import com.mindmap.assessment.service.AnswerService;
import com.mindmap.assessment.service.SafetyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnswerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Answer Submission Controller Tests")
class AnswerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AnswerService answerService;
    @MockBean private SafetyService safetyService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UserDetailsService userDetailsService;

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID QUESTION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OPTION_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    private void setUpSecurityContext() {
        UserPrincipal principal = new UserPrincipal(
                USER_ID,
                "john.doe",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("POST /sessions/{id}/answers")
    class SubmitAnswer {

        @Test
        @DisplayName("Should submit answer successfully")
        void submitAnswer_Success_Returns200() throws Exception {
            setUpSecurityContext();

            SubmitAnswerRequest request =
                    new SubmitAnswerRequest(QUESTION_ID, OPTION_ID, 2500);

            SubmitAnswerResponse response = new SubmitAnswerResponse(
                    true,
                    ProgressDto.of(6, 15),
                    null,
                    null
            );

            when(answerService.submitAnswer(
                    eq(USER_ID), eq(SESSION_ID), any(SubmitAnswerRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/sessions/{id}/answers", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accepted").value(true))
                    .andExpect(jsonPath("$.progress.answered").value(6))
                    .andExpect(jsonPath("$.progress.total").value(15));
        }

        @Test
        @DisplayName("Should submit a skipped answer successfully")
        void submitAnswer_Skipped_Returns200() throws Exception {
            setUpSecurityContext();

            // A null selectedOptionId represents a skip in this request DTO.
            SubmitAnswerRequest request =
                    new SubmitAnswerRequest(QUESTION_ID, null, 500);

            SubmitAnswerResponse response = new SubmitAnswerResponse(
                    true,
                    ProgressDto.of(7, 15),
                    null,
                    null
            );

            when(answerService.submitAnswer(
                    eq(USER_ID), eq(SESSION_ID), any(SubmitAnswerRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/sessions/{id}/answers", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accepted").value(true))
                    .andExpect(jsonPath("$.progress.answered").value(7));
        }

        @Test
        @DisplayName("Should return a safety alert when one is present")
        void submitAnswer_SafetyFlag_ReturnsSafetyAlert() throws Exception {
            setUpSecurityContext();

            SubmitAnswerRequest request =
                    new SubmitAnswerRequest(QUESTION_ID, OPTION_ID, 3000);

            SafetyAlert alert = new SafetyAlert(
                    "SUPPORT",
                    "If you are experiencing distress, please reach out for support.",
                    List.of("HELPLINE_NUMBER_PLACEHOLDER")
            );

            SubmitAnswerResponse response = new SubmitAnswerResponse(
                    true,
                    ProgressDto.of(8, 15),
                    null,
                    alert
            );

            when(answerService.submitAnswer(
                    eq(USER_ID), eq(SESSION_ID), any(SubmitAnswerRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/sessions/{id}/answers", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accepted").value(true))
                    .andExpect(jsonPath("$.safetyAlert.message")
                            .value("If you are experiencing distress, please reach out for support."));
        }
    }
}
