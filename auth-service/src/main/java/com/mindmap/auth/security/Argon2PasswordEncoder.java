package com.mindmap.auth.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-grade Argon2id PasswordEncoder using Bouncy Castle.
 * Default specification:
 * - Algorithm: Argon2id
 * - Memory: 65536 KB (64 MB)
 * - Iterations: 3
 * - Parallelism: 1
 * - Salt Length: 16 bytes
 * - Output Hash Length: 32 bytes
 *
 * Encoded format:
 * $argon2id$v=19$m=65536,t=3,p=1$&lt;salt_base64&gt;$&lt;hash_base64&gt;
 */
@Component
public class Argon2PasswordEncoder implements PasswordEncoder {

    private static final int DEFAULT_SALT_LENGTH = 16;
    private static final int DEFAULT_HASH_LENGTH = 32;
    private static final int DEFAULT_ITERATIONS = 3;
    private static final int DEFAULT_MEMORY_KB = 65536;
    private static final int DEFAULT_PARALLELISM = 1;

    private static final Pattern HASH_PATTERN = Pattern.compile(
        "^\\$argon2id\\$v=(\\d+)\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$([A-Za-z0-9+/=]+)\\$([A-Za-z0-9+/=]+)$"
    );

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder b64Encoder = Base64.getEncoder().withoutPadding();
    private final Base64.Decoder b64Decoder = Base64.getDecoder();

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }

        byte[] salt = new byte[DEFAULT_SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] hash = generateHash(
            rawPassword.toString().getBytes(StandardCharsets.UTF_8),
            salt,
            DEFAULT_ITERATIONS,
            DEFAULT_MEMORY_KB,
            DEFAULT_PARALLELISM,
            DEFAULT_HASH_LENGTH
        );

        return String.format(
            "$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
            DEFAULT_MEMORY_KB,
            DEFAULT_ITERATIONS,
            DEFAULT_PARALLELISM,
            b64Encoder.encodeToString(salt),
            b64Encoder.encodeToString(hash)
        );
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        Matcher matcher = HASH_PATTERN.matcher(encodedPassword);
        if (!matcher.matches()) {
            return false;
        }

        try {
            int memoryKb = Integer.parseInt(matcher.group(2));
            int iterations = Integer.parseInt(matcher.group(3));
            int parallelism = Integer.parseInt(matcher.group(4));
            byte[] salt = b64Decoder.decode(matcher.group(5));
            byte[] expectedHash = b64Decoder.decode(matcher.group(6));

            byte[] actualHash = generateHash(
                rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                salt,
                iterations,
                memoryKb,
                parallelism,
                expectedHash.length
            );

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] generateHash(byte[] password, byte[] salt, int iterations, int memoryKb, int parallelism, int outputLength) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memoryKb)
                .withParallelism(parallelism)
                .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        byte[] result = new byte[outputLength];
        generator.generateBytes(password, result, 0, result.length);
        return result;
    }
}
