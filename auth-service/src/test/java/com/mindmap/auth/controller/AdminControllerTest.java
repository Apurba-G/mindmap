package com.mindmap.auth.controller;

import com.mindmap.auth.dto.AdminUserResponse;
import com.mindmap.auth.entity.AuditLogEntry;
import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.repository.AuditLogRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Admin Controller Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        User adminUser = User.builder()
                .id(ADMIN_ID)
                .userId("admin.master")
                .displayName("Admin")
                .passwordHash("$argon2id$hash")
                .roles(new HashSet<>(Set.of(Role.ADMIN, Role.USER)))
                .isActive(true)
                .mustChangePassword(false)
                .build();

        when(userRepository.findByUserIdLower("admin.master")).thenReturn(Optional.of(adminUser));
    }

    // ─── LIST USERS ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users")
    class ListUsers {

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should return paginated user list for admin")
        void listUsers_AsAdmin_Returns200() throws Exception {
            AdminUserResponse user1 = new AdminUserResponse(
                    USER_ID, "john.doe", "John Doe", List.of("USER"),
                    true, false, false, 0, null, Instant.now(), Instant.now()
            );

            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(user1)));

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].userId").value("john.doe"))
                    .andExpect(jsonPath("$.content[0].isActive").value(true));
        }
    }

    // ─── GET USER BY ID ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users/{id}")
    class GetUserById {

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should return user details for admin")
        void getUserById_AsAdmin_Returns200() throws Exception {
            AdminUserResponse userResp = new AdminUserResponse(
                    USER_ID, "john.doe", "John Doe", List.of("USER"),
                    true, false, false, 0, null, Instant.now(), Instant.now()
            );

            when(userService.getUserById(USER_ID)).thenReturn(userResp);

            mockMvc.perform(get("/admin/users/{id}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("john.doe"))
                    .andExpect(jsonPath("$.failedLoginAttempts").value(0));
        }
    }

    // ─── UPDATE USER STATUS ──────────────────────────────────────

    @Nested
    @DisplayName("PATCH /admin/users/{id}/status")
    class UpdateUserStatus {

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should deactivate a user account")
        void updateUserStatus_Deactivate_Returns200() throws Exception {
            AdminUserResponse response = new AdminUserResponse(
                    USER_ID, "john.doe", "John Doe", List.of("USER"),
                    false, false, false, 0, null, Instant.now(), Instant.now()
            );

            when(userService.updateUserStatus(eq(USER_ID), eq(false), eq(ADMIN_ID), anyString()))
                    .thenReturn(response);

            mockMvc.perform(patch("/admin/users/{id}/status", USER_ID)
                            .param("active", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false));
        }

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should activate a user account")
        void updateUserStatus_Activate_Returns200() throws Exception {
            AdminUserResponse response = new AdminUserResponse(
                    USER_ID, "john.doe", "John Doe", List.of("USER"),
                    true, false, false, 0, null, Instant.now(), Instant.now()
            );

            when(userService.updateUserStatus(eq(USER_ID), eq(true), eq(ADMIN_ID), anyString()))
                    .thenReturn(response);

            mockMvc.perform(patch("/admin/users/{id}/status", USER_ID)
                            .param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(true));
        }
    }

    // ─── UNLOCK USER ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /admin/users/{id}/unlock")
    class UnlockUser {

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should unlock a locked user account")
        void unlockUser_AsAdmin_Returns200() throws Exception {
            AdminUserResponse response = new AdminUserResponse(
                    USER_ID, "john.doe", "John Doe", List.of("USER"),
                    true, false, false, 0, null, Instant.now(), Instant.now()
            );

            when(userService.unlockUser(eq(USER_ID), eq(ADMIN_ID), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/admin/users/{id}/unlock", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lockedUntil").isEmpty());
        }
    }

    // ─── AUDIT LOGS ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/audit-logs")
    class AuditLogs {

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should return audit log entries")
        void getAuditLogs_Returns200() throws Exception {
            when(auditLogRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/admin/audit-logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "admin.master", roles = "ADMIN")
        @DisplayName("Should filter audit logs by actor ID")
        void getAuditLogs_FilterByActor_Returns200() throws Exception {
            when(auditLogRepository.findByActorId(eq(ADMIN_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/admin/audit-logs")
                            .param("actorId", ADMIN_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
