package com.mindmap.profiling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.profiling.dto.CreateScoringRuleRequest;
import com.mindmap.profiling.dto.ScoringRuleDto;
import com.mindmap.profiling.dto.UpdateScoringRuleRequest;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.RuleType;
import com.mindmap.profiling.security.JwtAuthenticationFilter;
import com.mindmap.profiling.service.ScoringRuleService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoringRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Scoring Rule Controller Tests (Admin)")
class ScoringRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScoringRuleService scoringRuleService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final UUID RULE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    // ─── LIST RULES ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /admin/scoring-rules")
    class ListRules {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return all scoring rules for admin")
        void listAllRules_AsAdmin_Returns200() throws Exception {
            ScoringRuleDto rule = new ScoringRuleDto(
                    RULE_ID, ProfileLevel.LEVEL_1, "openness", "Big Five Openness",
                    RuleType.WEIGHT, Map.of("weight", 1.0), true, 1
            );

            when(scoringRuleService.getAllRules()).thenReturn(List.of(rule));

            mockMvc.perform(get("/admin/scoring-rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ruleName").value("Big Five Openness"))
                    .andExpect(jsonPath("$[0].traitKey").value("openness"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter rules by level")
        void listRulesByLevel_Returns200() throws Exception {
            ScoringRuleDto rule = new ScoringRuleDto(
                    RULE_ID, ProfileLevel.LEVEL_2, "ambiguity_tolerance", "Ambiguity Tolerance",
                    RuleType.FORMULA, Map.of("scale", 100.0), true, 1
            );

            when(scoringRuleService.getRulesByLevel(ProfileLevel.LEVEL_2)).thenReturn(List.of(rule));

            mockMvc.perform(get("/admin/scoring-rules")
                            .param("level", "LEVEL_2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].level").value("LEVEL_2"));
        }
    }

    // ─── CREATE RULE ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /admin/scoring-rules")
    class CreateRule {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create a new scoring rule")
        void createRule_Returns201() throws Exception {
            CreateScoringRuleRequest request = new CreateScoringRuleRequest(
                    ProfileLevel.LEVEL_1, "conscientiousness", "Big Five Conscientiousness",
                    RuleType.WEIGHT, Map.of("weight", 1.5), true
            );

            ScoringRuleDto created = new ScoringRuleDto(
                    UUID.randomUUID(), ProfileLevel.LEVEL_1, "conscientiousness",
                    "Big Five Conscientiousness", RuleType.WEIGHT, Map.of("weight", 1.5), true, 1
            );

            when(scoringRuleService.createRule(any(CreateScoringRuleRequest.class))).thenReturn(created);

            mockMvc.perform(post("/admin/scoring-rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ruleName").value("Big Five Conscientiousness"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }
    }

    // ─── UPDATE RULE ─────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /admin/scoring-rules/{id}")
    class UpdateRule {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update scoring rule parameters")
        void updateRule_Returns200() throws Exception {
            UpdateScoringRuleRequest request = new UpdateScoringRuleRequest(
                    Map.of("weight", 2.0), false
            );

            ScoringRuleDto updated = new ScoringRuleDto(
                    RULE_ID, ProfileLevel.LEVEL_1, "openness", "Big Five Openness",
                    RuleType.WEIGHT, Map.of("weight", 2.0), false, 2
            );

            when(scoringRuleService.updateRule(eq(RULE_ID), any(UpdateScoringRuleRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(put("/admin/scoring-rules/{id}", RULE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false))
                    .andExpect(jsonPath("$.version").value(2));
        }
    }

    // ─── DELETE RULE ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /admin/scoring-rules/{id}")
    class DeleteRule {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete scoring rule and return 204")
        void deleteRule_Returns204() throws Exception {
            doNothing().when(scoringRuleService).deleteRule(RULE_ID);

            mockMvc.perform(delete("/admin/scoring-rules/{id}", RULE_ID))
                    .andExpect(status().isNoContent());
        }
    }
}
