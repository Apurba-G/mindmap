package com.mindmap.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.security.Argon2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service that bootstraps initial seed administrator accounts on application startup
 * based on the ADMIN_USERS environment variable or application configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSeedService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RecoveryParagraphService recoveryParagraphService;
    private final UserIdService userIdService;
    private final Argon2PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${mindmap.security.admin-users:#{null}}")
    private String adminUsersJson;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String envAdmins = System.getenv("ADMIN_USERS");
        String configSource = envAdmins != null && !envAdmins.isBlank() ? envAdmins : adminUsersJson;

        if (configSource == null || configSource.isBlank()) {
            log.info("No ADMIN_USERS environment variable detected. Skipping admin seed initialization.");
            return;
        }

        seedAdmins(configSource);
    }

    /**
     * Seeds admin accounts parsed from a JSON payload.
     *
     * @param json JSON array of objects with userId and password/passwordHash
     */
    public void seedAdmins(String json) {
        try {
            List<Map<String, String>> adminConfigs = objectMapper.readValue(
                    json,
                    new TypeReference<>() {}
            );

            for (Map<String, String> config : adminConfigs) {
                String userId = config.get("userId");
                if (userId == null || userId.isBlank()) {
                    continue;
                }

                String userIdLower = userId.trim().toLowerCase(Locale.ROOT);
                if (userRepository.existsByUserIdLower(userIdLower)) {
                    log.info("Admin user '{}' already exists in database. Skipping seed.", userId);
                    continue;
                }

                String passwordHash = config.get("passwordHash");
                if (passwordHash == null || passwordHash.isBlank()) {
                    String rawPassword = config.get("password");
                    if (rawPassword != null && !rawPassword.isBlank()) {
                        passwordHash = passwordEncoder.encode(rawPassword);
                    } else {
                        // Generate random secure password if none provided
                        String generatedPassword = UUID.randomUUID().toString() + "!A1";
                        passwordHash = passwordEncoder.encode(generatedPassword);
                        log.warn("No password provided for admin '{}'. Generated temporary password: {}", userId, generatedPassword);
                    }
                }

                String plainRecoveryParagraph = recoveryParagraphService.generateParagraph();
                String recoveryHash = recoveryParagraphService.hashParagraph(plainRecoveryParagraph);

                User adminUser = User.builder()
                        .userId(userId.trim())
                        .userIdLower(userIdLower)
                        .displayName("Administrator (" + userId + ")")
                        .passwordHash(passwordHash)
                        .recoveryHash(recoveryHash)
                        .mustChangePassword(true)
                        .isActive(true)
                        .isAdminSeeded(true)
                        .failedLoginAttempts(0)
                        .roles(new HashSet<>(Set.of(Role.USER, Role.ADMIN)))
                        .build();

                User savedAdmin = userRepository.save(adminUser);
                userIdService.addToBloom(userIdLower);

                log.info("================================================================================");
                log.info("SEEDED INITIAL ADMIN ACCOUNT: {}", savedAdmin.getUserId());
                log.info("RECOVERY PARAGRAPH (RECORD THIS IMMEDIATELY - SHOWN ONLY ONCE):");
                log.info("{}", plainRecoveryParagraph);
                log.info("================================================================================");
            }
        } catch (Exception e) {
            log.error("Failed to parse and seed admin accounts from ADMIN_USERS: {}", e.getMessage(), e);
        }
    }
}
