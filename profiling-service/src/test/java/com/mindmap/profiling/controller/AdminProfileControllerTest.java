package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.FullProfileResponse;
import com.mindmap.profiling.dto.LevelScoresResponse;
import com.mindmap.profiling.dto.TemperamentResponse;
import com.mindmap.profiling.entity.ProfileAccessAudit;
import com.mindmap.profiling.security.JwtAuthenticationFilter;
import com.mindmap.profiling.security.UserPrincipal;
import com.mindmap.profiling.service.ProfileAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Admin Profile Controller Tests")
class AdminProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileAccessService profileAccessService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TARGET_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private void setUpAdminContext() {
        UserPrincipal principal = new UserPrincipal(
                ADMIN_ID, "admin.master",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─── FULL PROFILE ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users/{id}/full")
    class FullProfile {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return full diagnostic profile for admin")
        void getFullProfile_AsAdmin_Returns200() throws Exception {
            setUpAdminContext();

            FullProfileResponse response = new FullProfileResponse(
                    TARGET_USER_ID, "john.doe",
                    Map.of("openness", 72.0, "conscientiousness", 85.0, "extraversion", 45.0,
                            "agreeableness", 68.0, "emotional_stability", 60.0),
                    Map.of("ambiguity_tolerance", 65.0, "dark_theme_tolerance", 55.0, "moral_flexibility", 48.0),
                    Map.of("strong_and_calm", 60.0, "moderate_temper", 30.0, "low_control_angry", 5.0, "low_control_confused", 5.0),
                    0.87, Instant.now()
            );

            when(profileAccessService.getFullProfile(eq(TARGET_USER_ID), eq(ADMIN_ID), eq("admin.master"), anyString()))
                    .thenReturn(response);

            mockMvc.perform(get("/admin/users/{id}/full", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("john.doe"))
                    .andExpect(jsonPath("$.level1Scores.openness").value(72.0))
                    .andExpect(jsonPath("$.confidenceScore").value(0.87));
        }
    }

    // ─── TEMPERAMENT ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/users/{id}/temperament")
    class Temperament {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return temperament profile with blended percentages")
        void getTemperament_AsAdmin_Returns200() throws Exception {
            setUpAdminContext();

            TemperamentResponse response = new TemperamentResponse(
                    TARGET_USER_ID,
                    Map.of("strong_and_calm", 60.0, "moderate_temper", 30.0,
                            "low_control_angry", 5.0, "low_control_confused", 5.0),
                    "Strong and Calm",
                    "Demonstrates high emotional control with clear thinking under pressure.",
                    0.92
            );

            when(profileAccessService.getTemperament(eq(TARGET_USER_ID), eq(ADMIN_ID), eq("admin.master"), anyString()))
                    .thenReturn(response);

            mockMvc.perform(get("/admin/users/{id}/temperament", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.primaryType").value("Strong and Calm"))
                    .andExpect(jsonPath("$.percentages.strong_and_calm").value(60.0))
                    .andExpect(jsonPath("$.explanation").isNotEmpty());
        }
    }

    // ─── AUDIT TRAIL ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/audit-trail")
    class AuditTrail {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return profile access audit log")
        void getAuditTrail_Returns200() throws Exception {
            setUpAdminContext();

            when(profileAccessService.getAuditTrail(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(get("/admin/audit-trail"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
