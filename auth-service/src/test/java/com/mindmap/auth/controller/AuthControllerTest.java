package com.mindmap.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.auth.dto.LoginRequest;
import com.mindmap.auth.dto.LoginResponse;
import com.mindmap.auth.dto.RegisterRequest;
import com.mindmap.auth.dto.RegisterResponse;
import com.mindmap.auth.exception.AccountLockedException;
import com.mindmap.auth.exception.InvalidCredentialsException;
import com.mindmap.auth.exception.UserIdTakenException;
import com.mindmap.auth.security.JwtAuthenticationFilter;
import com.mindmap.auth.service.AuthenticationService;
import com.mindmap.auth.service.RegistrationService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    // ─── REGISTRATION ────────────────────────────────────────────

    @Nested
    @DisplayName("POST /register")
    class Registration {

        @Test
        @DisplayName("Should register user successfully and return 201")
        void register_Success_Returns201() throws Exception {
            RegisterRequest request = new RegisterRequest("john.doe", "John Doe", "Str0ngP@ss!", "Str0ngP@ss!");
            RegisterResponse response = new RegisterResponse(
                    UUID.randomUUID(), "john.doe",
                    "the quick brown fox jumps over the lazy dog near the river bank under the bright sun",
                    "eyJ...", "Bearer", 3600, "Registration successful"
            );

            when(registrationService.register(any(RegisterRequest.class), anyString())).thenReturn(response);

            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value("john.doe"))
                    .andExpect(jsonPath("$.recoveryParagraph").isNotEmpty())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.message").value("Registration successful"));
        }

        @Test
        @DisplayName("Should return 409 for duplicate user ID")
        void register_DuplicateUserId_Returns409() throws Exception {
            RegisterRequest request = new RegisterRequest("taken.user", "Test User", "Str0ngP@ss!", "Str0ngP@ss!");

            when(registrationService.register(any(), anyString()))
                    .thenThrow(new UserIdTakenException("User ID 'taken.user' is already taken"));

            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 for user ID shorter than 4 characters")
        void register_InvalidUserId_TooShort_Returns400() throws Exception {
            RegisterRequest request = new RegisterRequest("ab", "Test", "Str0ngP@ss!", "Str0ngP@ss!");

            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for user ID with invalid characters")
        void register_InvalidUserId_SpecialChars_Returns400() throws Exception {
            RegisterRequest request = new RegisterRequest("john@doe!", "Test", "Str0ngP@ss!", "Str0ngP@ss!");

            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void register_PasswordTooShort_Returns400() throws Exception {
            RegisterRequest request = new RegisterRequest("valid.user", "Test", "short", "short");

            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void register_MissingFields_Returns400() throws Exception {
            mockMvc.perform(post("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── LOGIN ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /login")
    class Login {

        @Test
        @DisplayName("Should login successfully and return JWT token")
        void login_Success_Returns200() throws Exception {
            LoginRequest request = new LoginRequest("john.doe", "Str0ngP@ss!");
            LoginResponse response = new LoginResponse("eyJ...", "Bearer", 3600, List.of("USER"), false);

            when(authenticationService.login(any(LoginRequest.class), anyString())).thenReturn(response);

            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("eyJ..."))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.roles[0]").value("USER"))
                    .andExpect(jsonPath("$.mustChangePassword").value(false));
        }

        @Test
        @DisplayName("Should return 401 for wrong password")
        void login_WrongPassword_Returns401() throws Exception {
            LoginRequest request = new LoginRequest("john.doe", "wrongpassword");

            when(authenticationService.login(any(), anyString()))
                    .thenThrow(new InvalidCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 423 when account is locked")
        void login_AccountLocked_Returns423() throws Exception {
            LoginRequest request = new LoginRequest("locked.user", "anypassword");

            when(authenticationService.login(any(), anyString()))
                    .thenThrow(new AccountLockedException("Account locked due to repeated failed attempts"));

            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isLocked());
        }

        @Test
        @DisplayName("Should return mustChangePassword=true for admin first login")
        void login_AdminFirstLogin_ReturnsMustChangePassword() throws Exception {
            LoginRequest request = new LoginRequest("admin.master", "Admin!InitialP@ss");
            LoginResponse response = new LoginResponse("eyJ...", "Bearer", 3600, List.of("ADMIN"), true);

            when(authenticationService.login(any(), anyString())).thenReturn(response);

            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mustChangePassword").value(true))
                    .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        }

        @Test
        @DisplayName("Should return 400 when login fields are missing")
        void login_MissingFields_Returns400() throws Exception {
            mockMvc.perform(post("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── LOGOUT ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /logout")
    class Logout {

        @Test
        @DisplayName("Should return 200 with success message")
        void logout_Returns200() throws Exception {
            mockMvc.perform(post("/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }
}
