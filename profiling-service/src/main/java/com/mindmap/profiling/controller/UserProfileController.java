package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.CompletionStatusResponse;
import com.mindmap.profiling.dto.ProfileExportDto;
import com.mindmap.profiling.dto.UserProfileSummaryResponse;
import com.mindmap.profiling.security.UserPrincipal;
import com.mindmap.profiling.service.ProfileAccessService;
import com.mindmap.profiling.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling user-facing profile endpoints.
 * Users are strictly scoped to their own profile data via ABAC enforcement.
 */
@Slf4j
@RestController
@RequestMapping({"/me", "/api/v1/profiles/me"})
@RequiredArgsConstructor
public class UserProfileController {

    private final ProfileAccessService profileAccessService;
    private final ProfileService profileService;

    /**
     * Retrieves the authenticated user's psychological profile summary.
     *
     * @param principal authenticated user context
     * @return non-clinical empathetic profile summary
     */
    @GetMapping
    public ResponseEntity<UserProfileSummaryResponse> getMyProfileSummary(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("User {} requesting personal profile summary", principal.getUserId());
        UserProfileSummaryResponse response = profileAccessService.getUserSummary(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the assessment completion status for the authenticated user.
     *
     * @param principal authenticated user context
     * @return completion status breakdown
     */
    @GetMapping("/completion")
    public ResponseEntity<CompletionStatusResponse> getMyCompletionStatus(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("User {} requesting assessment completion status", principal.getUserId());
        CompletionStatusResponse response = profileService.getCompletionStatus(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Exports full personal data for portability under GDPR standards.
     *
     * @param principal authenticated user context
     * @return exported profile data
     */
    @GetMapping("/export")
    public ResponseEntity<ProfileExportDto> exportMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("User {} requesting profile export", principal.getUserId());
        ProfileExportDto export = profileService.exportUserProfile(principal.getUserId());
        return ResponseEntity.ok(export);
    }

    /**
     * Deletes the authenticated user's entire psychological profile data under GDPR right to erasure.
     *
     * @param principal authenticated user context
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("User {} requested profile deletion", principal.getUserId());
        profileService.deleteUserProfile(principal.getUserId());
    }
}
