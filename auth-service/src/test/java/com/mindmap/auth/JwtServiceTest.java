package com.mindmap.auth;

import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        jwtService = new JwtService(testSecret, 3600, 300, "mindmap-test");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .userId("testuser")
                .displayName("Test User")
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token with correct claims")
    void testGenerateAccessToken() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);

        Claims claims = jwtService.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals(testUser.getId().toString(), claims.get("userId"));
        assertEquals("Test User", claims.get("displayName"));
        assertEquals("ACCESS", claims.get("type"));

        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRecoveryToken(token));
        assertEquals(testUser.getId(), jwtService.getUserIdFromToken(token));
        assertEquals("testuser", jwtService.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Should generate valid recovery token marked with type RECOVERY")
    void testGenerateRecoveryToken() {
        String token = jwtService.generateRecoveryToken(testUser);
        assertNotNull(token);

        Claims claims = jwtService.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals("RECOVERY", claims.get("type"));

        assertTrue(jwtService.isRecoveryToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }
}
