package com.mindmap.auth;

import com.mindmap.auth.security.Argon2PasswordEncoder;
import com.mindmap.auth.service.RecoveryParagraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryParagraphServiceTest {

    private RecoveryParagraphService recoveryParagraphService;

    @BeforeEach
    void setUp() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder();
        recoveryParagraphService = new RecoveryParagraphService(encoder);
        recoveryParagraphService.init();
    }

    @Test
    @DisplayName("Should generate a readable recovery paragraph with 40 to 60 words")
    void testGenerateParagraph() {
        String paragraph = recoveryParagraphService.generateParagraph();
        assertNotNull(paragraph);
        assertFalse(paragraph.isBlank());

        String[] words = paragraph.split("\\s+");
        assertTrue(words.length >= 35 && words.length <= 65,
                "Expected word count to be around 40-60 words, but was: " + words.length);

        // Verify punctuation presence (full stops terminating sentences)
        assertTrue(paragraph.endsWith("."));
        long periodCount = paragraph.chars().filter(ch -> ch == '.').count();
        assertEquals(4, periodCount, "Expected 4 sentences in paragraph");
    }

    @Test
    @DisplayName("Should normalize paragraph by lowercasing, stripping punctuation and collapsing whitespace")
    void testNormalizeParagraph() {
        String raw = "The Brave Knight,  danced across... The Forest!  ";
        String normalized = recoveryParagraphService.normalizeParagraph(raw);
        assertEquals("the brave knight danced across the forest", normalized);
    }

    @Test
    @DisplayName("Should hash and verify recovery paragraph successfully")
    void testHashAndVerifyParagraph() {
        String originalParagraph = recoveryParagraphService.generateParagraph();
        String hash = recoveryParagraphService.hashParagraph(originalParagraph);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$argon2id$"));

        // Verify exact match
        assertTrue(recoveryParagraphService.verifyParagraph(originalParagraph, hash));

        // Verify match despite case differences or extra punctuation
        String alteredCaseAndPunctuation = originalParagraph.toUpperCase() + "!!!";
        assertTrue(recoveryParagraphService.verifyParagraph(alteredCaseAndPunctuation, hash));

        // Verify mismatch with incorrect paragraph
        assertFalse(recoveryParagraphService.verifyParagraph("completely different text here", hash));
    }
}
