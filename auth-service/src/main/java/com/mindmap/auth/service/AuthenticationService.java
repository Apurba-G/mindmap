package com.mindmap.auth.service;

import com.mindmap.auth.dto.LoginRequest;
import com.mindmap.auth.dto.LoginResponse;
import com.mindmap.auth.entity.LoginAttempt;
import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.exception.AccountLockedException;
import com.mindmap.auth.exception.InvalidCredentialsException;
import com.mindmap.auth.exception.RateLimitExceededException;
import com.mindmap.auth.repository.LoginAttemptRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service handling user login, Redis-backed rate limiting, Argon2 credential verification,
 * progressive account lockout, and login attempt logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final Argon2PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${mindmap.rate-limiting.login.user-max-attempts:5}")
    private int userMaxAttempts;

    @Value("${mindmap.rate-limiting.login.user-window-seconds:900}")
    private long userWindowSeconds;

    @Value("${mindmap.rate-limiting.login.ip-max-attempts:20}")
    private int ipMaxAttempts;

    @Value("${mindmap.rate-limiting.login.ip-window-seconds:900}")
    private long ipWindowSeconds;

    @Value("${mindmap.rate-limiting.login.lockout-duration-minutes:15}")
    private long lockoutDurationMinutes;

    /**
     * Authenticates a user with rate limit checks, password verification, and lockout logic.
     *
     * @param request credentials payload
     * @param ipAddress client IP address
     * @return LoginResponse containing the access token and permissions
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String rawUserId = request.userId().trim();
        String userIdLower = rawUserId.toLowerCase(Locale.ROOT);

        // 1. Check Redis rate limits (IP & user key)
        checkRateLimits(userIdLower, ipAddress);

        // 2. Fetch user
        Optional<User> userOpt = userRepository.findByUserIdLower(userIdLower);

        if (userOpt.isEmpty()) {
            recordLoginAttempt(userIdLower, ipAddress, false, "USER_NOT_FOUND");
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();

        // 3. Check account lock
        if (user.isAccountLocked()) {
            recordLoginAttempt(user.getUserId(), ipAddress, false, "ACCOUNT_LOCKED");
            throw new AccountLockedException("Account is temporarily locked. Try again later.", user.getLockedUntil());
        }

        // 4. Check account active status
        if (!user.isActive()) {
            recordLoginAttempt(user.getUserId(), ipAddress, false, "ACCOUNT_DISABLED");
            throw new InvalidCredentialsException("Account has been deactivated. Please contact support.");
        }

        // 5. Verify password
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!passwordMatches) {
            handleFailedLogin(user, ipAddress);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // 6. Handle successful login
        handleSuccessfulLogin(user, ipAddress);

        // 7. Generate JWT access token
        String accessToken = jwtService.generateToken(user);

        List<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                roles,
                user.isMustChangePassword()
        );
    }

    private void handleFailedLogin(User user, String ipAddress) {
        int failedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= userMaxAttempts) {
            Instant lockedUntil = Instant.now().plus(Duration.ofMinutes(lockoutDurationMinutes));
            user.setLockedUntil(lockedUntil);
            log.warn("User '{}' reached {} failed attempts. Account locked until {}", user.getUserId(), failedAttempts, lockedUntil);
        }

        userRepository.save(user);
        recordLoginAttempt(user.getUserId(), ipAddress, false, "INVALID_PASSWORD");

        auditService.log(
                user.getId(),
                user.getUserId(),
                "LOGIN_FAILED",
                "USER",
                user.getId().toString(),
                Map.of("attempts", failedAttempts, "locked", user.isAccountLocked()),
                ipAddress
        );
    }

    private void handleSuccessfulLogin(User user, String ipAddress) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Clear rate-limit counter in Redis
        try {
            stringRedisTemplate.delete("rate:login:user:" + user.getUserIdLower());
        } catch (Exception e) {
            log.debug("Redis rate-limit cleanup failed: {}", e.getMessage());
        }

        recordLoginAttempt(user.getUserId(), ipAddress, true, null);

        auditService.log(
                user.getId(),
                user.getUserId(),
                "LOGIN_SUCCESS",
                "USER",
                user.getId().toString(),
                Map.of("roles", user.getRoles()),
                ipAddress
        );

        log.info("User '{}' authenticated successfully.", user.getUserId());
    }

    private void checkRateLimits(String userIdLower, String ipAddress) {
        try {
            String ipKey = "rate:login:ip:" + (ipAddress != null ? ipAddress : "unknown");
            Long ipCount = stringRedisTemplate.opsForValue().increment(ipKey);
            if (ipCount != null && ipCount == 1L) {
                stringRedisTemplate.expire(ipKey, Duration.ofSeconds(ipWindowSeconds));
            }
            if (ipCount != null && ipCount > ipMaxAttempts) {
                throw new RateLimitExceededException("Too many login attempts from this IP address. Please wait.", ipWindowSeconds);
            }

            String userKey = "rate:login:user:" + userIdLower;
            Long userCount = stringRedisTemplate.opsForValue().increment(userKey);
            if (userCount != null && userCount == 1L) {
                stringRedisTemplate.expire(userKey, Duration.ofSeconds(userWindowSeconds));
            }
            if (userCount != null && userCount > userMaxAttempts) {
                throw new RateLimitExceededException("Too many login attempts for this account. Please wait.", userWindowSeconds);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Redis rate limit evaluation bypassed: {}", e.getMessage());
        }
    }

    private void recordLoginAttempt(String userId, String ipAddress, boolean successful, String failureReason) {
        try {
            LoginAttempt attempt = LoginAttempt.builder()
                    .userId(userId)
                    .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                    .successful(successful)
                    .failureReason(failureReason)
                    .attemptedAt(Instant.now())
                    .build();
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            log.error("Failed to persist LoginAttempt: {}", e.getMessage());
        }
    }
}
