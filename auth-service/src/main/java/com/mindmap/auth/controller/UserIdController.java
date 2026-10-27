package com.mindmap.auth.controller;

import com.mindmap.auth.dto.UserIdCheckResponse;
import com.mindmap.auth.dto.UserIdSuggestionsResponse;
import com.mindmap.auth.service.UserIdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for User ID availability checking and smart suggestions.
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "User ID", description = "Endpoints for username availability checking and smart suggestions")
public class UserIdController {

    private final UserIdService userIdService;

    @Operation(summary = "Check availability of a User ID")
    @GetMapping({"/userid/check", "/api/v1/auth/userid/check"})
    public ResponseEntity<UserIdCheckResponse> checkUserId(@RequestParam("userId") String userId) {
        boolean available = userIdService.checkAvailability(userId);
        String checkedVia = "REDIS_BLOOM_AND_DB";
        return ResponseEntity.ok(new UserIdCheckResponse(userId, available, checkedVia));
    }

    @Operation(summary = "Generate 5 unique username suggestions based on name and interests")
    @GetMapping({"/userid/suggestions", "/api/v1/auth/userid/suggestions"})
    public ResponseEntity<UserIdSuggestionsResponse> getSuggestions(
            @RequestParam("name") String name,
            @RequestParam(value = "interests", required = false) List<String> interests
    ) {
        List<String> suggestions = userIdService.generateSuggestions(name, interests);
        return ResponseEntity.ok(new UserIdSuggestionsResponse(suggestions));
    }
}
