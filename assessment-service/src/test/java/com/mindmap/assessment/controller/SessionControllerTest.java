package com.mindmap.assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.assessment.dto.CreateSessionRequest;
import com.mindmap.assessment.dto.SessionResponse;
import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.security.JwtAuthenticationFilter;
import com.mindmap.assessment.security.UserPrincipal;
import com.mindmap.assessment.service.CompletionService;
import com.mindmap.assessment.service.SessionService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Assessment Session Controller Tests")
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SessionService sessionService;
    @MockBean private CompletionService completionService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UserDetailsService userDetailsService;

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

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
    @DisplayName("POST /sessions")
    class CreateSession {

        @Test
        void createSession_Authenticated_Returns201() throws Exception {
            setUpSecurityContext();

            CreateSessionRequest request =
                    new CreateSessionRequest(AssessmentLevel.LEVEL_1);

            SessionResponse response = new SessionResponse(
                    SESSION_ID,
                    AssessmentLevel.LEVEL_1,
                    15,
                    Collections.emptyList(),
                    null
            );

            when(sessionService.createSession(
                    eq(USER_ID), eq(AssessmentLevel.LEVEL_1)))
                    .thenReturn(response);

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.level").value("LEVEL_1"))
                    .andExpect(jsonPath("$.totalQuestions").value(15));
        }
    }

    @Nested
    @DisplayName("GET /sessions/current")
    class GetCurrentSession {

        @Test
        void getCurrentSession_Returns200() throws Exception {
            setUpSecurityContext();

            SessionResponse response = new SessionResponse(
                    SESSION_ID,
                    AssessmentLevel.LEVEL_1,
                    15,
                    Collections.emptyList(),
                    null
            );

            when(sessionService.resumeSession(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/sessions/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()));
        }
    }

    @Nested
    @DisplayName("GET /sessions/{id}")
    class GetSessionById {

        @Test
        void getSessionById_Returns200() throws Exception {
            setUpSecurityContext();

            SessionResponse response = new SessionResponse(
                    SESSION_ID,
                    AssessmentLevel.LEVEL_2,
                    10,
                    Collections.emptyList(),
                    null
            );

            when(sessionService.getSession(USER_ID, SESSION_ID))
                    .thenReturn(response);

            mockMvc.perform(get("/sessions/{id}", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.level").value("LEVEL_2"));
        }
    }

    @Nested
    @DisplayName("POST /sessions/{id}/complete")
    class CompleteSession {

        @Test
        void completeSession_Returns204() throws Exception {
            setUpSecurityContext();

            doNothing().when(completionService)
                    .completeLevel(USER_ID, SESSION_ID);

            mockMvc.perform(post("/sessions/{id}/complete", SESSION_ID))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /sessions/{id}/abandon")
    class AbandonSession {

        @Test
        void abandonSession_Returns204() throws Exception {
            setUpSecurityContext();

            doNothing().when(sessionService)
                    .abandonSession(USER_ID, SESSION_ID);

            mockMvc.perform(post("/sessions/{id}/abandon", SESSION_ID))
                    .andExpect(status().isNoContent());
        }
    }
}
