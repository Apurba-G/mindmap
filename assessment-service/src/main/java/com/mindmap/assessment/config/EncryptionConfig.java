package com.mindmap.assessment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Cryptographic utility providing AES-256-GCM authenticated encryption
 * for sensitive assessment answer data.
 */
@Slf4j
@Configuration
public class EncryptionConfig {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionConfig(@Value("${app.encryption.secret-key:MySuperSecretAssessmentKey2026!}") String secretKeyStr) {
        this.secretKey = deriveKey(secretKeyStr);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Derives a 256-bit AES key from a passphrase using SHA-256.
     */
    private SecretKey deriveKey(String rawKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize AES-256 key", e);
        }
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     *
     * @param plaintext data to encrypt
     * @return combined byte array containing IV prepended to ciphertext
     */
    public byte[] encrypt(String plaintext) {
        if (plaintext == null) {
            return new byte[0];
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return byteBuffer.array();
        } catch (Exception e) {
            log.error("Error encrypting assessment payload", e);
            throw new IllegalStateException("Failed to encrypt sensitive response data", e);
        }
    }

    /**
     * Decrypts byte array payload using AES-256-GCM.
     *
     * @param encryptedData IV prepended to ciphertext
     * @return plaintext decrypted string
     */
    public String decrypt(byte[] encryptedData) {
        if (encryptedData == null || encryptedData.length <= GCM_IV_LENGTH) {
            return null;
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting assessment payload", e);
            throw new IllegalStateException("Failed to decrypt sensitive response data", e);
        }
    }
}
