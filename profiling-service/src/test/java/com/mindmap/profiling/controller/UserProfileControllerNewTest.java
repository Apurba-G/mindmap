package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.CompletionStatusResponse;
import com.mindmap.profiling.dto.ProfileExportDto;
import com.mindmap.profiling.dto.UserProfileSummaryResponse;
import com.mindmap.profiling.dto.SummaryItemDto;
import com.mindmap.profiling.security.JwtAuthenticationFilter;
import com.mindmap.profiling.security.UserPrincipal;
import com.mindmap.profiling.service.ProfileAccessService;
import com.mindmap.profiling.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("User Profile Controller Tests")
class UserProfileControllerNewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileAccessService profileAccessService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private void setUpUserContext() {
        UserPrincipal principal = new UserPrincipal(
                USER_ID, "john.doe",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─── GET MY PROFILE SUMMARY ──────────────────────────────────

    @Nested
    @DisplayName("GET /me")
    class GetProfileSummary {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return gentle non-clinical summary for user")
        void getMyProfileSummary_Returns200() throws Exception {
            setUpUserContext();

            UserProfileSummaryResponse response = new UserProfileSummaryResponse(
                    true,
                    "LEVEL_3",
                    87.0,
                    List.of(new SummaryItemDto(
                            "LEVEL_1",
                            "You show a balanced personality with strong analytical thinking.",
                            "SHORT"
                    )),
                    UserProfileSummaryResponse.DEFAULT_DISCLAIMER
            );

            when(profileAccessService.getUserSummary(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summaries").isArray())
                    .andExpect(jsonPath("$.disclaimer").value("For self-awareness only, not a clinical diagnosis."));
        }
    }

    // ─── COMPLETION STATUS ───────────────────────────────────────

    @Nested
    @DisplayName("GET /me/completion")
    class CompletionStatus {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return assessment completion status")
        void getCompletionStatus_Returns200() throws Exception {
            setUpUserContext();

            CompletionStatusResponse response = new CompletionStatusResponse(
                    USER_ID,
                    false,
                    "LEVEL_2",
                    80.0,
                    List.of("LEVEL_1", "LEVEL_2"),
                    Instant.now()
            );

            when(profileService.getCompletionStatus(USER_ID)).thenReturn(response);

            mockMvc.perform(get("/me/completion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.highestLevel").value("LEVEL_2"))
                    .andExpect(jsonPath("$.completedLevels[0]").value("LEVEL_1"))
                    .andExpect(jsonPath("$.completedLevels[1]").value("LEVEL_2"));
        }
    }

    // ─── EXPORT PROFILE ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /me/export")
    class ExportProfile {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should export user profile data (GDPR)")
        void exportProfile_Returns200() throws Exception {
            setUpUserContext();

            ProfileExportDto export = new ProfileExportDto(
                    UUID.randomUUID(),
                    USER_ID,
                    false,
                    Instant.now(),
                    Instant.now(),
                    List.of()
            );

            when(profileService.exportUserProfile(USER_ID)).thenReturn(export);

            mockMvc.perform(get("/me/export"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
        }
    }

    // ─── DELETE PROFILE ──────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /me")
    class DeleteProfile {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should delete user profile and return 204")
        void deleteProfile_Returns204() throws Exception {
            setUpUserContext();

            doNothing().when(profileService).deleteUserProfile(USER_ID);

            mockMvc.perform(delete("/me"))
                    .andExpect(status().isNoContent());
        }
    }
}
