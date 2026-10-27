package com.mindmap.auth.service;

import com.mindmap.auth.dto.RegisterRequest;
import com.mindmap.auth.dto.RegisterResponse;
import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.exception.UserIdInvalidException;
import com.mindmap.auth.exception.UserIdTakenException;
import com.mindmap.auth.exception.WeakPasswordException;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.security.Argon2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service orchestrating new user registration, credential hashing, recovery phrase generation,
 * Bloom filter updates, and initial JWT token issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]");

    private final UserRepository userRepository;
    private final UserIdService userIdService;
    private final RecoveryParagraphService recoveryParagraphService;
    private final Argon2PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    /**
     * Registers a new user account.
     *
     * @param request the registration details
     * @param ipAddress client IP address
     * @return RegisterResponse containing the access token and the unhashed recovery paragraph
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request, String ipAddress) {
        String rawUserId = request.userId().trim();
        String userIdLower = rawUserId.toLowerCase(Locale.ROOT);

        // 1. Validate password confirmation
        if (!request.password().equals(request.passwordConfirm())) {
            throw new WeakPasswordException("Password and password confirmation do not match");
        }

        // 2. Validate password complexity
        validatePasswordStrength(request.password());

        // 3. Validate user ID format and availability
        if (rawUserId.length() < 4 || rawUserId.length() > 20) {
            throw new UserIdInvalidException("User ID must be between 4 and 20 characters");
        }

        if (!userIdService.checkAvailability(rawUserId)) {
            throw new UserIdTakenException(rawUserId, "The username is already taken, reserved, or has an invalid format");
        }

        // 4. Hash password with Argon2id
        String passwordHash = passwordEncoder.encode(request.password());

        // 5. Generate and hash recovery paragraph
        String plainRecoveryParagraph = recoveryParagraphService.generateParagraph();
        String recoveryHash = recoveryParagraphService.hashParagraph(plainRecoveryParagraph);

        // 6. Persist user entity
        User user = User.builder()
                .userId(rawUserId)
                .userIdLower(userIdLower)
                .displayName(request.displayName().trim())
                .passwordHash(passwordHash)
                .recoveryHash(recoveryHash)
                .mustChangePassword(false)
                .isActive(true)
                .isAdminSeeded(false)
                .failedLoginAttempts(0)
                .roles(Set.of(Role.USER))
                .build();

        User savedUser = userRepository.save(user);

        // 7. Update Bloom filter
        userIdService.addToBloom(userIdLower);

        // 8. Generate JWT access token
        String accessToken = jwtService.generateToken(savedUser);

        // 9. Audit event
        auditService.log(
                savedUser.getId(),
                savedUser.getUserId(),
                "USER_REGISTERED",
                "USER",
                savedUser.getId().toString(),
                Map.of("displayName", savedUser.getDisplayName(), "roles", savedUser.getRoles()),
                ipAddress
        );

        log.info("Successfully registered user '{}' with ID {}", savedUser.getUserId(), savedUser.getId());

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUserId(),
                plainRecoveryParagraph,
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                "Account created successfully. Please record your recovery paragraph securely; it cannot be recovered if lost."
        );
    }

    /**
     * Validates that the password meets enterprise complexity requirements:
     * - Minimum 8 characters, maximum 128
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special symbol
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new WeakPasswordException("Password must be between 8 and 128 characters long");
        }
        if (!UPPER_CASE_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        if (!LOWER_CASE_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one digit");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one special character (!@#$%^&*...)");
        }
    }
}
