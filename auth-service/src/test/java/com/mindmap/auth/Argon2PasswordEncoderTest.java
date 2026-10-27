package com.mindmap.auth;

import com.mindmap.auth.security.Argon2PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Argon2PasswordEncoderTest {

    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder();

    @Test
    @DisplayName("Should encode password and successfully match original password")
    void testEncodeAndMatch() {
        String rawPassword = "ComplexPassword#2026!";
        String encoded = encoder.encode(rawPassword);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$argon2id$v=19$m=65536,t=3,p=1$"));

        assertTrue(encoder.matches(rawPassword, encoded));
        assertFalse(encoder.matches("WrongPassword#2026!", encoded));
    }

    @Test
    @DisplayName("Should generate unique hashes for identical passwords due to salting")
    void testSaltingUniqueness() {
        String password = "ConsistentPassword123$";
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        assertNotEquals(hash1, hash2);
        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }
}
