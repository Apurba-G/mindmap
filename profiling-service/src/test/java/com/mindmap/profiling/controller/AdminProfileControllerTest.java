package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.FullProfileResponse;
import com.mindmap.profiling.dto.LevelScoresResponse;
import com.mindmap.profiling.dto.TemperamentResponse;
import com.mindmap.profiling.entity.ProfileAccessAudit;
import com.mindmap.profiling.entity.TemperamentType;
import com.mindmap.profiling.security.JwtAuthenticationFilter;
import com.mindmap.profiling.security.UserPrincipal;
import com.mindmap.profiling.service.ProfileAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.isNull;
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
                    TARGET_USER_ID,
                    "john.doe",
                    true,
                    87.0,
                    new LevelScoresResponse("LEVEL_1", Map.of()),
                    new LevelScoresResponse("LEVEL_2", Map.of()),
                    new LevelScoresResponse("LEVEL_3", Map.of()),
                    null,
                    List.of(),
                    FullProfileResponse.ADMIN_DISCLAIMER
            );

            when(profileAccessService.getFullProfile(eq(TARGET_USER_ID), eq(ADMIN_ID), eq("admin.master"), anyString()))
                    .thenReturn(response);

            mockMvc.perform(get("/admin/users/{id}/full", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userHandle").value("john.doe"))
                    .andExpect(jsonPath("$.level1Scores.level").value("LEVEL_1"))
                    .andExpect(jsonPath("$.overallConfidence").value(87.0));
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
                    TemperamentType.STRONG_AND_CALM,
                    Map.of(
                            TemperamentType.STRONG_AND_CALM, 60.0,
                            TemperamentType.MODERATE_TEMPER, 30.0,
                            TemperamentType.LOW_CONTROL_ANGRY, 5.0,
                            TemperamentType.LOW_CONTROL_CONFUSED, 5.0
                    ),
                    "Demonstrates high emotional control with clear thinking under pressure.",
                    92.0
            );

            when(profileAccessService.getTemperament(eq(TARGET_USER_ID), eq(ADMIN_ID), eq("admin.master"), anyString()))
                    .thenReturn(response);

            mockMvc.perform(get("/admin/users/{id}/temperament", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.primaryType").value("STRONG_AND_CALM"))
                    .andExpect(jsonPath("$.breakdownPercentages.STRONG_AND_CALM").value(60.0))
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

            when(profileAccessService.getAuditLogs(isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            mockMvc.perform(get("/admin/audit-trail"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
