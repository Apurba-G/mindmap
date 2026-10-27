package com.mindmap.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.auth.dto.RecoveryInitiateRequest;
import com.mindmap.auth.dto.RecoveryInitiateResponse;
import com.mindmap.auth.dto.ResetPasswordRequest;
import com.mindmap.auth.dto.ResetPasswordResponse;
import com.mindmap.auth.exception.RecoveryFailedException;
import com.mindmap.auth.security.JwtAuthenticationFilter;
import com.mindmap.auth.service.JwtService;
import com.mindmap.auth.service.RecoveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Account Recovery Controller Tests")
class RecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecoveryService recoveryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // ─── INITIATE RECOVERY ───────────────────────────────────────

    @Nested
    @DisplayName("POST /recovery/initiate")
    class InitiateRecovery {

        @Test
        @DisplayName("Should return recovery token when paragraph is valid")
        void initiateRecovery_ValidParagraph_Returns200() throws Exception {
            RecoveryInitiateRequest request = new RecoveryInitiateRequest(
                    "john.doe",
                    "the quick brown fox jumps over the lazy dog near the river bank under the bright sun"
            );
            RecoveryInitiateResponse response = new RecoveryInitiateResponse("recovery-jwt-token", 300);

            when(recoveryService.initiateRecovery(any(RecoveryInitiateRequest.class), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/recovery/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recoveryToken").value("recovery-jwt-token"))
                    .andExpect(jsonPath("$.expiresIn").value(300));
        }

        @Test
        @DisplayName("Should return 401 when paragraph is wrong")
        void initiateRecovery_WrongParagraph_Returns401() throws Exception {
            RecoveryInitiateRequest request = new RecoveryInitiateRequest("john.doe", "wrong paragraph text");

            when(recoveryService.initiateRecovery(any(), anyString()))
                    .thenThrow(new RecoveryFailedException("Invalid recovery paragraph"));

            mockMvc.perform(post("/recovery/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when fields are missing")
        void initiateRecovery_MissingFields_Returns400() throws Exception {
            mockMvc.perform(post("/recovery/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── RESET PASSWORD ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /recovery/reset")
    class ResetPassword {

        @Test
        @DisplayName("Should reset password with valid recovery token and return new paragraph")
        void resetPassword_WithValidToken_Returns200() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("NewStr0ngP@ss!", "NewStr0ngP@ss!");
            ResetPasswordResponse response = new ResetPasswordResponse(
                    "Password reset successfully",
                    "new recovery paragraph generated for the user with high entropy words forming sentences",
                    "Save this new recovery paragraph. It cannot be shown again."
            );

            when(jwtService.isRecoveryToken("valid-recovery-token")).thenReturn(true);
            when(jwtService.getUserIdFromToken("valid-recovery-token")).thenReturn(USER_ID);
            when(recoveryService.resetPassword(any(ResetPasswordRequest.class), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/recovery/reset")
                            .header("X-Recovery-Token", "valid-recovery-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully"))
                    .andExpect(jsonPath("$.newRecoveryParagraph").isNotEmpty())
                    .andExpect(jsonPath("$.warning").isNotEmpty());
        }

        @Test
        @DisplayName("Should return error when token is not a recovery token")
        void resetPassword_WithInvalidTokenType_ReturnsError() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("NewStr0ngP@ss!", "NewStr0ngP@ss!");

            when(jwtService.isRecoveryToken("regular-jwt")).thenReturn(false);

            mockMvc.perform(post("/recovery/reset")
                            .header("X-Recovery-Token", "regular-jwt")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return error when no token is provided")
        void resetPassword_MissingToken_ReturnsError() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("NewStr0ngP@ss!", "NewStr0ngP@ss!");

            mockMvc.perform(post("/recovery/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when new password is too short")
        void resetPassword_PasswordTooShort_Returns400() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("short", "short");

            mockMvc.perform(post("/recovery/reset")
                            .header("X-Recovery-Token", "valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
