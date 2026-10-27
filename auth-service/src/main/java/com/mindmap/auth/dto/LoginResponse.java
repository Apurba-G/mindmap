package com.mindmap.auth.dto;

import java.util.List;

/**
 * Response payload returned upon successful login.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    List<String> roles,
    boolean mustChangePassword
) {}
