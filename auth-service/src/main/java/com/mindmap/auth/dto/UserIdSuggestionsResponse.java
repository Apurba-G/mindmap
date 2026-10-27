package com.mindmap.auth.dto;

import java.util.List;

/**
 * Response payload containing available User ID suggestions.
 */
public record UserIdSuggestionsResponse(
    List<String> suggestions
) {}
