package com.mindmap.assessment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionConfigTest {

    private EncryptionConfig encryptionConfig;

    @BeforeEach
    void setUp() {
        encryptionConfig = new EncryptionConfig("TestSecretKeyForAes256EncryptionGcmMode2026!");
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt raw assessment answer payload")
    void shouldEncryptAndDecryptPayload() {
        String originalPayload = "{\"questionId\":\"123e4567-e89b-12d3-a456-426614174000\",\"optionId\":\"opt-1\",\"traitKey\":\"EXTROVERSION\"}";

        byte[] encrypted = encryptionConfig.encrypt(originalPayload);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 12);
        assertNotEquals(originalPayload, new String(encrypted));

        String decrypted = encryptionConfig.decrypt(encrypted);
        assertEquals(originalPayload, decrypted);
    }
}
