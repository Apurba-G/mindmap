package com.mindmap.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.assessment.dto.SessionData;
import com.mindmap.assessment.entity.AssessmentLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing fast in-memory session tracking, caching, and progress counters via Redis Hashes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionService {

    private static final String KEY_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_LEVEL = "level";
    private static final String FIELD_PROGRESS = "progress";
    private static final String FIELD_ANSWERED_COUNT = "answeredCount";
    private static final String FIELD_TOTAL_QUESTIONS = "totalQuestions";
    private static final String FIELD_CURRENT_QUESTION = "currentQuestion";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_ANSWERS = "answers";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private String getKey(UUID sessionId) {
        return KEY_PREFIX + sessionId.toString();
    }

    /**
     * Initializes a Redis session hash with initial progress, timing, and empty answer collection.
     */
    public void createSession(UUID sessionId, SessionData data) {
        String key = getKey(sessionId);
        Map<String, String> hash = new HashMap<>();
        hash.put(FIELD_SESSION_ID, sessionId.toString());
        hash.put(FIELD_USER_ID, data.userId().toString());
        hash.put(FIELD_LEVEL, data.level().name());
        hash.put(FIELD_PROGRESS, data.answeredCount() + "/" + data.totalQuestions());
        hash.put(FIELD_ANSWERED_COUNT, String.valueOf(data.answeredCount()));
        hash.put(FIELD_TOTAL_QUESTIONS, String.valueOf(data.totalQuestions()));
        if (data.currentQuestionId() != null) {
            hash.put(FIELD_CURRENT_QUESTION, data.currentQuestionId().toString());
        }
        hash.put(FIELD_START_TIME, String.valueOf(data.startTimeEpochMs()));

        try {
            String serializedAnswers = objectMapper.writeValueAsString(data.answers() != null ? data.answers() : Map.of());
            hash.put(FIELD_ANSWERS, serializedAnswers);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize answers for session {}", sessionId, e);
            hash.put(FIELD_ANSWERS, "{}");
        }

        redisTemplate.opsForHash().putAll(key, hash);
        redisTemplate.expire(key, SESSION_TTL);
        log.info("Initialized Redis session hash for session: {}", sessionId);
    }

    /**
     * Loads session data from Redis hash.
     *
     * @param sessionId the session UUID
     * @return SessionData or null if session not cached
     */
    public SessionData getSession(UUID sessionId) {
        String key = getKey(sessionId);
        Map<Object, Object> rawHash = redisTemplate.opsForHash().entries(key);
        if (rawHash == null || rawHash.isEmpty()) {
            return null;
        }

        UUID id = UUID.fromString((String) rawHash.get(FIELD_SESSION_ID));
        UUID userId = UUID.fromString((String) rawHash.get(FIELD_USER_ID));
        AssessmentLevel level = AssessmentLevel.valueOf((String) rawHash.get(FIELD_LEVEL));
        int answeredCount = Integer.parseInt((String) rawHash.getOrDefault(FIELD_ANSWERED_COUNT, "0"));
        int totalQuestions = Integer.parseInt((String) rawHash.getOrDefault(FIELD_TOTAL_QUESTIONS, "0"));

        String currentQStr = (String) rawHash.get(FIELD_CURRENT_QUESTION);
        UUID currentQuestionId = (currentQStr != null && !currentQStr.isBlank()) ? UUID.fromString(currentQStr) : null;

        long startTimeEpochMs = Long.parseLong((String) rawHash.getOrDefault(FIELD_START_TIME, "0"));

        Map<String, String> answers = new HashMap<>();
        String answersJson = (String) rawHash.get(FIELD_ANSWERS);
        if (answersJson != null && !answersJson.isBlank()) {
            try {
                answers = objectMapper.readValue(answersJson, new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize answers for session {}", sessionId, e);
            }
        }

        return new SessionData(id, userId, level, answeredCount, totalQuestions, currentQuestionId, startTimeEpochMs, answers);
    }

    /**
     * Updates progress counters in the Redis session hash.
     */
    public void updateProgress(UUID sessionId, int answered, int total) {
        String key = getKey(sessionId);
        Map<String, String> updates = new HashMap<>();
        updates.put(FIELD_PROGRESS, answered + "/" + total);
        updates.put(FIELD_ANSWERED_COUNT, String.valueOf(answered));
        updates.put(FIELD_TOTAL_QUESTIONS, String.valueOf(total));
        redisTemplate.opsForHash().putAll(key, updates);
        redisTemplate.expire(key, SESSION_TTL);
    }

    /**
     * Records an answered question with selected option and response time into the Redis hash.
     */
    public void recordAnswer(UUID sessionId, UUID questionId, UUID optionId, int responseTimeMs) {
        String key = getKey(sessionId);
        String answersJson = (String) redisTemplate.opsForHash().get(key, FIELD_ANSWERS);
        Map<String, String> answers = new HashMap<>();
        if (answersJson != null && !answersJson.isBlank()) {
            try {
                answers = objectMapper.readValue(answersJson, new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize answers for session {}", sessionId, e);
            }
        }

        String value = (optionId != null ? optionId.toString() : "SKIPPED") + ":" + responseTimeMs;
        answers.put(questionId.toString(), value);

        try {
            redisTemplate.opsForHash().put(key, FIELD_ANSWERS, objectMapper.writeValueAsString(answers));
            redisTemplate.expire(key, SESSION_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize updated answers for session {}", sessionId, e);
        }
    }

    /**
     * Updates the current active question pointer for the session in Redis.
     */
    public void setCurrentQuestion(UUID sessionId, UUID nextQuestionId) {
        String key = getKey(sessionId);
        if (nextQuestionId != null) {
            redisTemplate.opsForHash().put(key, FIELD_CURRENT_QUESTION, nextQuestionId.toString());
        } else {
            redisTemplate.opsForHash().delete(key, FIELD_CURRENT_QUESTION);
        }
        redisTemplate.expire(key, SESSION_TTL);
    }

    /**
     * Deletes the session hash from Redis when finished or abandoned.
     */
    public void deleteSession(UUID sessionId) {
        String key = getKey(sessionId);
        redisTemplate.delete(key);
        log.info("Deleted Redis session hash for session: {}", sessionId);
    }
}
