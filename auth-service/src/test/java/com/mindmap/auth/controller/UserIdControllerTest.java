package com.mindmap.auth.controller;

import com.mindmap.auth.dto.UserIdCheckResponse;
import com.mindmap.auth.dto.UserIdSuggestionsResponse;
import com.mindmap.auth.security.JwtAuthenticationFilter;
import com.mindmap.auth.service.UserIdService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserIdController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("User ID Availability Controller Tests")
class UserIdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdService userIdService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    // ─── CHECK AVAILABILITY ──────────────────────────────────────

    @Nested
    @DisplayName("GET /userid/check")
    class CheckAvailability {

        @Test
        @DisplayName("Should return available=true for unused user ID")
        void checkUserId_Available_ReturnsTrue() throws Exception {
            when(userIdService.checkAvailability("fresh.user")).thenReturn(true);

            mockMvc.perform(get("/userid/check").param("userId", "fresh.user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("fresh.user"))
                    .andExpect(jsonPath("$.available").value(true))
                    .andExpect(jsonPath("$.checkedVia").value("REDIS_BLOOM_AND_DB"));
        }

        @Test
        @DisplayName("Should return available=false for taken user ID")
        void checkUserId_Taken_ReturnsFalse() throws Exception {
            when(userIdService.checkAvailability("taken.user")).thenReturn(false);

            mockMvc.perform(get("/userid/check").param("userId", "taken.user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("taken.user"))
                    .andExpect(jsonPath("$.available").value(false));
        }
    }

    // ─── SUGGESTIONS ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /userid/suggestions")
    class Suggestions {

        @Test
        @DisplayName("Should return 5 suggestions from name")
        void getSuggestions_ReturnsListOfSuggestions() throws Exception {
            List<String> suggestions = List.of("john.doe42", "john_doe", "j.doe2024", "johnd.99", "doe.john");
            when(userIdService.generateSuggestions("John Doe", null)).thenReturn(suggestions);

            mockMvc.perform(get("/userid/suggestions").param("name", "John Doe"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suggestions").isArray())
                    .andExpect(jsonPath("$.suggestions.length()").value(5))
                    .andExpect(jsonPath("$.suggestions[0]").value("john.doe42"));
        }

        @Test
        @DisplayName("Should return suggestions with interests")
        void getSuggestions_WithInterests_ReturnsSuggestions() throws Exception {
            List<String> suggestions = List.of("john.music", "doe.art42", "john.code", "music.doe", "art.john");
            when(userIdService.generateSuggestions("John Doe", List.of("music", "art")))
                    .thenReturn(suggestions);

            mockMvc.perform(get("/userid/suggestions")
                            .param("name", "John Doe")
                            .param("interests", "music", "art"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suggestions").isArray())
                    .andExpect(jsonPath("$.suggestions.length()").value(5));
        }
    }
}
