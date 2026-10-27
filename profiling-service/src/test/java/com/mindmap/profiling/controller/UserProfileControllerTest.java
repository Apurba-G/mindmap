package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.CompletionStatusResponse;
import com.mindmap.profiling.dto.UserProfileSummaryResponse;
import com.mindmap.profiling.security.JwtAuthenticationFilter;
import com.mindmap.profiling.security.UserPrincipal;
import com.mindmap.profiling.service.ProfileAccessService;
import com.mindmap.profiling.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileAccessService profileAccessService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /me returns user profile summary")
    void testGetMyProfileSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "testuser", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        UserProfileSummaryResponse summaryResponse = new UserProfileSummaryResponse(
                false, "LEVEL_1", 90.0, Collections.emptyList(), UserProfileSummaryResponse.DEFAULT_DISCLAIMER
        );

        when(profileAccessService.getUserSummary(userId)).thenReturn(summaryResponse);

        mockMvc.perform(get("/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highestLevel").value("LEVEL_1"))
                .andExpect(jsonPath("$.overallConfidence").value(90.0));
    }

    @Test
    @DisplayName("GET /me/completion returns completion status")
    void testGetMyCompletionStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "testuser", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        CompletionStatusResponse statusResponse = new CompletionStatusResponse(
                userId, true, "LEVEL_3", 88.5, List.of("LEVEL_1", "LEVEL_2", "LEVEL_3"), Instant.now()
        );

        when(profileService.getCompletionStatus(userId)).thenReturn(statusResponse);

        mockMvc.perform(get("/me/completion").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isComplete").value(true))
                .andExpect(jsonPath("$.highestLevel").value("LEVEL_3"));
    }
}
