package com.mindmap.profiling.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utility component for validating and extracting authentication details from JSON Web Tokens (JJWT 0.12.x).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${profiling.security.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${profiling.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes;
            if (jwtSecret.length() >= 32) {
                keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            } else {
                // Ensure sufficient entropy for HMAC-SHA256
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            }
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize JWT key digest", e);
        }
    }

    /**
     * Validates a JWT string.
     *
     * @param token JWT token string
     * @return true if valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts an {@link Authentication} object from the given JWT token.
     *
     * @param token JWT token string
     * @return Spring Security Authentication object
     */
    @SuppressWarnings("unchecked")
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        UUID userId = UUID.fromString(subject);

        String handle = claims.get("handle", String.class);
        if (handle == null) {
            handle = claims.get("username", String.class);
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> roleList) {
            for (Object role : roleList) {
                String roleStr = role.toString();
                if (!roleStr.startsWith("ROLE_")) {
                    roleStr = "ROLE_" + roleStr;
                }
                authorities.add(new SimpleGrantedAuthority(roleStr));
            }
        } else {
            String singleRole = claims.get("role", String.class);
            if (singleRole != null) {
                if (!singleRole.startsWith("ROLE_")) {
                    singleRole = "ROLE_" + singleRole;
                }
                authorities.add(new SimpleGrantedAuthority(singleRole));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
        }

        UserPrincipal principal = new UserPrincipal(userId, handle, authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * Helper method to generate a test JWT token for a specific user and role.
     *
     * @param userId user identifier
     * @param handle user handle
     * @param roles  list of roles
     * @return generated JWT string
     */
    public String generateToken(UUID userId, String handle, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("handle", handle)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
}
