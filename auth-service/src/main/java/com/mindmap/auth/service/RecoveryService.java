package com.mindmap.auth.service;

import com.mindmap.auth.dto.RecoveryInitiateRequest;
import com.mindmap.auth.dto.RecoveryInitiateResponse;
import com.mindmap.auth.dto.ResetPasswordRequest;
import com.mindmap.auth.dto.ResetPasswordResponse;
import com.mindmap.auth.entity.RecoveryAttempt;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.exception.RateLimitExceededException;
import com.mindmap.auth.exception.RecoveryFailedException;
import com.mindmap.auth.exception.ResourceNotFoundException;
import com.mindmap.auth.exception.WeakPasswordException;
import com.mindmap.auth.repository.RecoveryAttemptRepository;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.security.Argon2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating paragraph-based account recovery and secure password reset.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final UserRepository userRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryParagraphService recoveryParagraphService;
    private final RegistrationService registrationService;
    private final Argon2PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${mindmap.rate-limiting.recovery.user-max-attempts:3}")
    private int userMaxAttempts;

    @Value("${mindmap.rate-limiting.recovery.user-window-seconds:3600}")
    private long userWindowSeconds;

    @Value("${mindmap.rate-limiting.recovery.ip-max-attempts:10}")
    private int ipMaxAttempts;

    @Value("${mindmap.rate-limiting.recovery.ip-window-seconds:3600}")
    private long ipWindowSeconds;

    /**
     * Initiates password recovery by verifying the user's secret recovery paragraph.
     *
     * @param request recovery payload
     * @param ipAddress client IP address
     * @return RecoveryInitiateResponse containing a short-lived recovery token (5 minutes)
     */
    @Transactional
    public RecoveryInitiateResponse initiateRecovery(RecoveryInitiateRequest request, String ipAddress) {
        String rawUserId = request.userId().trim();
        String userIdLower = rawUserId.toLowerCase(Locale.ROOT);

        // 1. Rate limiting check
        checkRecoveryRateLimits(userIdLower, ipAddress);

        // 2. Fetch user
        Optional<User> userOpt = userRepository.findByUserIdLower(userIdLower);

        if (userOpt.isEmpty()) {
            recordRecoveryAttempt(userIdLower, ipAddress, false, "USER_NOT_FOUND");
            throw new RecoveryFailedException("Account recovery verification failed. Please verify your username and recovery paragraph.");
        }

        User user = userOpt.get();

        // 3. Verify recovery paragraph
        boolean isValid = recoveryParagraphService.verifyParagraph(request.recoveryParagraph(), user.getRecoveryHash());

        if (!isValid) {
            recordRecoveryAttempt(user.getUserId(), ipAddress, false, "INVALID_RECOVERY_PARAGRAPH");
            auditService.log(
                    user.getId(),
                    user.getUserId(),
                    "RECOVERY_VERIFICATION_FAILED",
                    "USER",
                    user.getId().toString(),
                    Map.of("reason", "Paragraph mismatch"),
                    ipAddress
            );
            throw new RecoveryFailedException("Account recovery verification failed. The supplied paragraph does not match our records.");
        }

        // 4. Record successful verification
        recordRecoveryAttempt(user.getUserId(), ipAddress, true, null);

        // 5. Generate short-lived recovery token
        String recoveryToken = jwtService.generateRecoveryToken(user);

        auditService.log(
                user.getId(),
                user.getUserId(),
                "RECOVERY_VERIFICATION_SUCCESS",
                "USER",
                user.getId().toString(),
                Map.of("tokenType", "RECOVERY"),
                ipAddress
        );

        log.info("Recovery verification succeeded for user '{}'. Recovery token issued.", user.getUserId());

        return new RecoveryInitiateResponse(
                recoveryToken,
                jwtService.getRecoveryTokenExpirationSeconds()
        );
    }

    /**
     * Resets the user's password using the recovery token and automatically rotates the recovery paragraph.
     *
     * @param request new password payload
     * @param userId user UUID extracted from the recovery token
     * @return ResetPasswordResponse with new recovery paragraph
     */
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request, UUID userId) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new WeakPasswordException("New password and confirmation do not match");
        }

        registrationService.validatePasswordStrength(request.newPassword());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));

        // Hash new password
        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        // Generate and hash NEW rotated recovery paragraph
        String newRecoveryParagraph = recoveryParagraphService.generateParagraph();
        String newRecoveryHash = recoveryParagraphService.hashParagraph(newRecoveryParagraph);

        // Update user state: unlock, reset attempts, clear mustChangePassword
        user.setPasswordHash(newPasswordHash);
        user.setRecoveryHash(newRecoveryHash);
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);

        auditService.log(
                user.getId(),
                user.getUserId(),
                "PASSWORD_RESET_VIA_RECOVERY",
                "USER",
                user.getId().toString(),
                Map.of("status", "SUCCESS"),
                "SYSTEM"
        );

        log.info("Password successfully reset via recovery token for user '{}'.", user.getUserId());

        return new ResetPasswordResponse(
                "Password has been reset successfully.",
                newRecoveryParagraph,
                "IMPORTANT: Your previous recovery paragraph has been invalidated. Please store this new recovery paragraph safely."
        );
    }

    private void checkRecoveryRateLimits(String userIdLower, String ipAddress) {
        try {
            String ipKey = "rate:recovery:ip:" + (ipAddress != null ? ipAddress : "unknown");
            Long ipCount = stringRedisTemplate.opsForValue().increment(ipKey);
            if (ipCount != null && ipCount == 1L) {
                stringRedisTemplate.expire(ipKey, Duration.ofSeconds(ipWindowSeconds));
            }
            if (ipCount != null && ipCount > ipMaxAttempts) {
                throw new RateLimitExceededException("Too many recovery attempts from this IP address. Please wait an hour.", ipWindowSeconds);
            }

            String userKey = "rate:recovery:user:" + userIdLower;
            Long userCount = stringRedisTemplate.opsForValue().increment(userKey);
            if (userCount != null && userCount == 1L) {
                stringRedisTemplate.expire(userKey, Duration.ofSeconds(userWindowSeconds));
            }
            if (userCount != null && userCount > userMaxAttempts) {
                throw new RateLimitExceededException("Too many recovery attempts for this account. Please wait an hour.", userWindowSeconds);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Redis recovery rate limiting evaluation bypassed: {}", e.getMessage());
        }
    }

    private void recordRecoveryAttempt(String userId, String ipAddress, boolean successful, String failureReason) {
        try {
            RecoveryAttempt attempt = RecoveryAttempt.builder()
                    .userId(userId)
                    .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                    .successful(successful)
                    .failureReason(failureReason)
                    .attemptedAt(Instant.now())
                    .build();
            recoveryAttemptRepository.save(attempt);
        } catch (Exception e) {
            log.error("Failed to persist RecoveryAttempt: {}", e.getMessage());
        }
    }
}
