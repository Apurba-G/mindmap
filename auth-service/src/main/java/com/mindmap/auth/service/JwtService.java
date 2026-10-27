package com.mindmap.auth.service;

import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for issuing and validating JSON Web Tokens (access tokens and recovery tokens) using JJWT.
 */
@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_DISPLAY_NAME = "displayName";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TYPE = "type";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_RECOVERY = "RECOVERY";

    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;
    private final long recoveryTokenExpirationSeconds;
    private final String issuer;

    public JwtService(
            @Value("${mindmap.jwt.secret}") String secret,
            @Value("${mindmap.jwt.access-token-expiration-seconds:3600}") long accessTokenExpirationSeconds,
            @Value("${mindmap.jwt.recovery-token-expiration-seconds:300}") long recoveryTokenExpirationSeconds,
            @Value("${mindmap.jwt.issuer:mindmap-auth-service}") String issuer
    ) {
        this.signingKey = buildKey(secret);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.recoveryTokenExpirationSeconds = recoveryTokenExpirationSeconds;
        this.issuer = issuer;
    }

    private SecretKey buildKey(String secret) {
        try {
            // If secret is hex or base64 or raw string, ensure at least 256 bits via SHA-256
            byte[] keyBytes;
            if (secret.length() >= 64 && secret.matches("^[0-9a-fA-F]+$")) {
                keyBytes = Decoders.BASE64.decode(Base64.getEncoder().encodeToString(hexStringToByteArray(secret)));
            } else if (secret.length() >= 44 && isBase64(secret)) {
                keyBytes = Decoders.BASE64.decode(secret);
            } else {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize JWT key with SHA-256", e);
        }
    }

    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Generates a standard access token for authenticated API requests.
     *
     * @param user the authenticated User entity
     * @return signed JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationSeconds);

        List<String> roleNames = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getUserId())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_DISPLAY_NAME, user.getDisplayName())
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a short-lived recovery token (5 minutes) for password reset.
     *
     * @param user the User entity undergoing recovery
     * @return signed recovery JWT string
     */
    public String generateRecoveryToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(recoveryTokenExpirationSeconds);

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getUserId())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_RECOVERY)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates and parses the claims from a JWT token.
     *
     * @param token the JWT string
     * @return Claims payload
     * @throws JwtException if token is invalid or expired
     */
    public Claims validateToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts username (subject) from a token.
     */
    public String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extracts the user's primary UUID from the token claims.
     */
    public UUID getUserIdFromToken(String token) {
        String idStr = validateToken(token).get(CLAIM_USER_ID, String.class);
        return UUID.fromString(idStr);
    }

    /**
     * Checks if the token is an access token (not a recovery token).
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = validateToken(token);
            String type = claims.get(CLAIM_TYPE, String.class);
            return TOKEN_TYPE_ACCESS.equals(type) || type == null;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Checks if the token is a dedicated password recovery token.
     */
    public boolean isRecoveryToken(String token) {
        try {
            Claims claims = validateToken(token);
            return TOKEN_TYPE_RECOVERY.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRecoveryTokenExpirationSeconds() {
        return recoveryTokenExpirationSeconds;
    }
}
