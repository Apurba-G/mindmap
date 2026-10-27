package com.mindmap.assessment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Validates and extracts claims from JSON Web Tokens issued by the Auth Service.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates whether the given JWT token is active and has valid signature.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts Claims from signed JWT token.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts UserPrincipal from JWT claims.
     */
    @SuppressWarnings("unchecked")
    public UserPrincipal getUserPrincipal(String token) {
        Claims claims = getClaims(token);

        UUID userId;
        Object userIdClaim = claims.get("userId");
        if (userIdClaim != null) {
            userId = UUID.fromString(userIdClaim.toString());
        } else {
            userId = UUID.fromString(claims.getSubject());
        }

        String email = claims.get("email", String.class);
        if (email == null) {
            email = claims.getSubject();
        }

        List<String> roles = new ArrayList<>();
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
        } else if (rolesObj instanceof String roleStr) {
            roles.add(roleStr);
        }

        Object authoritiesObj = claims.get("authorities");
        if (authoritiesObj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
        }

        return UserPrincipal.of(userId, email, roles);
    }
}
