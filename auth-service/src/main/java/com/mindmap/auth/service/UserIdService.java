package com.mindmap.auth.service;

import com.mindmap.auth.repository.ReservedUsernameRepository;
import com.mindmap.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for user ID format validation, availability checking via RedisBloom &amp; DB,
 * Bloom filter rebuilding, and smart user ID suggestion generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserIdService {

    private static final Pattern USER_ID_FORMAT_PATTERN = Pattern.compile("^[a-z0-9._]{4,20}$");

    private final UserRepository userRepository;
    private final ReservedUsernameRepository reservedUsernameRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${mindmap.bloom-filter.key:userid_bloom}")
    private String bloomFilterKey;

    @Value("${mindmap.bloom-filter.false-positive-rate:0.01}")
    private double falsePositiveRate;

    @Value("${mindmap.bloom-filter.expected-insertions:1000000}")
    private long expectedInsertions;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Checks if a User ID is available for registration.
     * Follows the verification pipeline:
     * 1. Format validation (regex [a-z0-9._]{4,20})
     * 2. Reserved username check in database
     * 3. RedisBloom BF.EXISTS check
     * 4. Database check if Bloom filter indicates potential existence
     *
     * @param userId raw or lowercased username
     * @return true if available, false if invalid or taken
     */
    public boolean checkAvailability(String userId) {
        if (userId == null) {
            return false;
        }

        String lowerUserId = userId.trim().toLowerCase(Locale.ROOT);

        // 1. Format check
        if (!USER_ID_FORMAT_PATTERN.matcher(lowerUserId).matches()) {
            return false;
        }

        // 2. Reserved words check
        if (reservedUsernameRepository.existsByWord(lowerUserId)) {
            return false;
        }

        // 3. RedisBloom check
        Boolean bloomExists = checkBloomFilter(lowerUserId);

        if (bloomExists != null) {
            if (!bloomExists) {
                // Bloom filter has 0% false negatives: definitely does not exist
                return true;
            }
            // Bloom filter reported exists (could be true positive or ~1% false positive)
            // Perform authoritative database verification
            return !userRepository.existsByUserIdLower(lowerUserId);
        }

        // Fallback if RedisBloom is unavailable: direct DB check
        return !userRepository.existsByUserIdLower(lowerUserId);
    }

    /**
     * Executes raw Redis command BF.EXISTS to test membership in Bloom filter.
     *
     * @param userIdLower the lowercase username
     * @return true if Bloom filter indicates existence, false if absent, null if RedisBloom unsupported
     */
    public Boolean checkBloomFilter(String userIdLower) {
        try {
            return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                byte[] keyBytes = bloomFilterKey.getBytes(StandardCharsets.UTF_8);
                byte[] itemBytes = userIdLower.getBytes(StandardCharsets.UTF_8);
                Object result = connection.execute("BF.EXISTS", keyBytes, itemBytes);
                if (result instanceof Number num) {
                    return num.longValue() == 1L;
                } else if (result instanceof Boolean b) {
                    return b;
                }
                return false;
            });
        } catch (Exception e) {
            log.debug("RedisBloom BF.EXISTS not available or failed: {}. Falling back to DB.", e.getMessage());
            return null;
        }
    }

    /**
     * Adds a newly registered user ID to the Redis Bloom filter.
     *
     * @param userIdLower the lowercase username
     */
    public void addToBloom(String userIdLower) {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                byte[] keyBytes = bloomFilterKey.getBytes(StandardCharsets.UTF_8);
                byte[] itemBytes = userIdLower.getBytes(StandardCharsets.UTF_8);
                return connection.execute("BF.ADD", keyBytes, itemBytes);
            });
        } catch (Exception e) {
            log.warn("Failed to add '{}' to Redis Bloom filter: {}", userIdLower, e.getMessage());
        }
    }

    /**
     * Rebuilds the Redis Bloom filter on startup by loading all existing User IDs from the database.
     */
    public void rebuildBloomFilter() {
        log.info("Rebuilding Bloom filter '{}' from database...", bloomFilterKey);

        // Reserve filter if not exists
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                byte[] keyBytes = bloomFilterKey.getBytes(StandardCharsets.UTF_8);
                byte[] errorRateBytes = String.valueOf(falsePositiveRate).getBytes(StandardCharsets.UTF_8);
                byte[] capacityBytes = String.valueOf(expectedInsertions).getBytes(StandardCharsets.UTF_8);
                return connection.execute("BF.RESERVE", keyBytes, errorRateBytes, capacityBytes);
            });
            log.info("Bloom filter reserved with error rate {} and capacity {}", falsePositiveRate, expectedInsertions);
        } catch (Exception e) {
            log.debug("BF.RESERVE skipped or filter already initialized: {}", e.getMessage());
        }

        List<String> allUserIds = userRepository.findAllUserIdLower();
        int added = 0;

        for (String idLower : allUserIds) {
            if (idLower != null && !idLower.isBlank()) {
                addToBloom(idLower);
                added++;
            }
        }

        log.info("Populated Redis Bloom filter with {} user IDs.", added);
    }

    /**
     * Generates 5 unique available username suggestions based on user name, interests, and random suffixes.
     *
     * @param name user display name or full name
     * @param interests optional list of user interests or topics
     * @return list of 5 available user ID suggestions
     */
    public List<String> generateSuggestions(String name, List<String> interests) {
        Set<String> suggestions = new LinkedHashSet<>();
        String sanitizedName = sanitizeName(name);

        String[] parts = sanitizedName.split("[._]");
        String first = parts.length > 0 ? parts[0] : "user";
        String last = parts.length > 1 ? parts[1] : "";

        List<String> candidates = new ArrayList<>();

        // Variations from name
        if (!last.isEmpty()) {
            candidates.add(first + "." + last);
            candidates.add(first + "_" + last);
            candidates.add(first.charAt(0) + last);
            candidates.add(first + last);
        } else {
            candidates.add(first);
        }

        // Add interest-based combinations
        if (interests != null && !interests.isEmpty()) {
            for (String interest : interests) {
                String cleanInterest = sanitizeName(interest);
                if (!cleanInterest.isEmpty()) {
                    candidates.add(first + "." + cleanInterest);
                    candidates.add(first + "_" + cleanInterest);
                    if (!last.isEmpty()) {
                        candidates.add(last + "_" + cleanInterest);
                    }
                }
            }
        }

        // Add candidates with random 2-digit numbers
        List<String> baseCandidates = new ArrayList<>(candidates);
        for (String base : baseCandidates) {
            int randomNum = 10 + secureRandom.nextInt(90);
            candidates.add(base + randomNum);
            candidates.add(base + "_" + randomNum);
        }

        // Filter valid candidates and check availability
        for (String candidate : candidates) {
            String trimmed = candidate.length() > 20 ? candidate.substring(0, 20) : candidate;
            if (trimmed.length() >= 4 && USER_ID_FORMAT_PATTERN.matcher(trimmed).matches()) {
                if (checkAvailability(trimmed)) {
                    suggestions.add(trimmed);
                    if (suggestions.size() >= 5) {
                        break;
                    }
                }
            }
        }

        // Fallback: If still under 5, generate random suffixes until we have 5
        while (suggestions.size() < 5) {
            int randomNum = 100 + secureRandom.nextInt(900);
            String candidate = first + randomNum;
            if (candidate.length() > 20) {
                candidate = candidate.substring(0, 16) + randomNum;
            }
            if (candidate.length() < 4) {
                candidate = candidate + "1234";
            }
            if (checkAvailability(candidate)) {
                suggestions.add(candidate);
            }
        }

        return new ArrayList<>(suggestions);
    }

    private String sanitizeName(String input) {
        if (input == null) {
            return "user";
        }
        String cleaned = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", ".")
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");
        return cleaned.isEmpty() ? "user" : cleaned;
    }
}
