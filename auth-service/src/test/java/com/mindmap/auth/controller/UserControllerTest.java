package com.mindmap.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.auth.dto.ChangePasswordRequest;
import com.mindmap.auth.dto.UserProfileResponse;
import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.security.JwtAuthenticationFilter;
import com.mindmap.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("User Self-Service Controller Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .userId("john.doe")
                .displayName("John Doe")
                .passwordHash("$argon2id$hash")
                .roles(new HashSet<>(Set.of(Role.USER)))
                .isActive(true)
                .mustChangePassword(false)
                .build();

        when(userRepository.findByUserIdLower("john.doe")).thenReturn(Optional.of(testUser));
    }

    // ─── GET PROFILE ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /me")
    class GetProfile {

        @Test
        @WithMockUser(username = "john.doe", roles = "USER")
        @DisplayName("Should return user profile for authenticated user")
        void getProfile_Authenticated_Returns200() throws Exception {
            UserProfileResponse response = new UserProfileResponse(
                    USER_ID, "john.doe", "John Doe", List.of("USER"), false, Instant.now()
            );

            when(userService.getProfile(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("john.doe"))
                    .andExpect(jsonPath("$.displayName").value("John Doe"))
                    .andExpect(jsonPath("$.mustChangePassword").value(false));
        }
    }

    // ─── CHANGE PASSWORD ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /me/change-password")
    class ChangePassword {

        @Test
        @WithMockUser(username = "john.doe", roles = "USER")
        @DisplayName("Should change password successfully")
        void changePassword_Success_Returns200() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "OldP@ssw0rd!", "NewStr0ngP@ss!", "NewStr0ngP@ss!"
            );

            doNothing().when(userService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class), anyString());

            mockMvc.perform(post("/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @WithMockUser(username = "john.doe", roles = "USER")
        @DisplayName("Should return 400 when new password is too short")
        void changePassword_NewPasswordTooShort_Returns400() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("OldP@ss!", "short", "short");

            mockMvc.perform(post("/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john.doe", roles = "USER")
        @DisplayName("Should return 400 when current password is blank")
        void changePassword_MissingCurrentPassword_Returns400() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("", "NewStr0ngP@ss!", "NewStr0ngP@ss!");

            mockMvc.perform(post("/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
