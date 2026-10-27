package com.mindmap.auth.service;

import com.mindmap.auth.dto.AdminUserResponse;
import com.mindmap.auth.dto.ChangePasswordRequest;
import com.mindmap.auth.dto.UserProfileResponse;
import com.mindmap.auth.entity.Role;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.exception.AdminIdImmutableException;
import com.mindmap.auth.exception.InvalidCredentialsException;
import com.mindmap.auth.exception.ResourceNotFoundException;
import com.mindmap.auth.exception.WeakPasswordException;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.security.Argon2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing user profile retrieval, self-service password changes,
 * and administrative account management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RegistrationService registrationService;
    private final Argon2PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /**
     * Retrieves the authenticated user's self profile.
     *
     * @param userId user UUID
     * @return UserProfileResponse
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        return toUserProfileResponse(user);
    }

    /**
     * Changes user password after verifying their current password.
     *
     * @param userId user UUID
     * @param request change password payload
     * @param ipAddress client IP address
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String ipAddress) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new WeakPasswordException("New password and confirmation do not match");
        }

        registrationService.validatePasswordStrength(request.newPassword());

        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditService.log(
                    user.getId(),
                    user.getUserId(),
                    "CHANGE_PASSWORD_FAILED",
                    "USER",
                    user.getId().toString(),
                    Map.of("reason", "Current password mismatch"),
                    ipAddress
            );
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(newPasswordHash);
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditService.log(
                user.getId(),
                user.getUserId(),
                "CHANGE_PASSWORD_SUCCESS",
                "USER",
                user.getId().toString(),
                Map.of("status", "SUCCESS"),
                ipAddress
        );

        log.info("User '{}' changed password successfully.", user.getUserId());
    }

    /**
     * Administrative endpoint to list all users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminUserResponse);
    }

    /**
     * Administrative endpoint to retrieve a single user by ID.
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID userId) {
        User user = findUserOrThrow(userId);
        return toAdminUserResponse(user);
    }

    /**
     * Administrative endpoint to activate or deactivate an account.
     * Seed admins cannot be deactivated.
     */
    @Transactional
    public AdminUserResponse updateUserStatus(UUID targetUserId, boolean active, UUID adminId, String ipAddress) {
        User user = findUserOrThrow(targetUserId);

        if (user.isAdminSeeded() && !active) {
            throw new AdminIdImmutableException("System seed administrator accounts cannot be deactivated.");
        }

        user.setActive(active);
        User saved = userRepository.save(user);

        auditService.log(
                adminId,
                "ADMIN",
                active ? "ACTIVATE_USER" : "DEACTIVATE_USER",
                "USER",
                targetUserId.toString(),
                Map.of("targetUser", user.getUserId(), "active", active),
                ipAddress
        );

        return toAdminUserResponse(saved);
    }

    /**
     * Administrative endpoint to unlock a locked user account.
     */
    @Transactional
    public AdminUserResponse unlockUser(UUID targetUserId, UUID adminId, String ipAddress) {
        User user = findUserOrThrow(targetUserId);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User saved = userRepository.save(user);

        auditService.log(
                adminId,
                "ADMIN",
                "UNLOCK_USER",
                "USER",
                targetUserId.toString(),
                Map.of("targetUser", user.getUserId()),
                ipAddress
        );

        return toAdminUserResponse(saved);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

        return new UserProfileResponse(
                user.getId(),
                user.getUserId(),
                user.getDisplayName(),
                roles,
                user.isMustChangePassword(),
                user.getCreatedAt()
        );
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

        return new AdminUserResponse(
                user.getId(),
                user.getUserId(),
                user.getDisplayName(),
                roles,
                user.isActive(),
                user.isAdminSeeded(),
                user.isMustChangePassword(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
