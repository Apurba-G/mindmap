package com.mindmap.assessment.controller;

import com.mindmap.assessment.dto.ContentWarningAckRequest;
import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentPart;
import com.mindmap.assessment.entity.ContentWarningAck;
import com.mindmap.assessment.security.SecurityUtils;
import com.mindmap.assessment.service.ContentWarningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for recording acknowledgments and consents for sensitive assessment content warnings.
 */
@Slf4j
@RestController
@RequestMapping("/content-warnings")
@RequiredArgsConstructor
public class ContentWarningController {

    private final ContentWarningService contentWarningService;

    /**
     * Acknowledges or accepts the content warning for a specific assessment level and part.
     */
    @PostMapping("/{level}/{part}/ack")
    public ResponseEntity<ContentWarningAck> acknowledgeContentWarning(
            @PathVariable("level") AssessmentLevel level,
            @PathVariable("part") AssessmentPart part,
            @Valid @RequestBody ContentWarningAckRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: acknowledge content warning for user {}, level {}, part {}, accepted {}",
                userId, level, part, request.accepted());

        ContentWarningAck ack = contentWarningService.acknowledgeWarning(
                userId, level, part, Boolean.TRUE.equals(request.accepted()));
        return ResponseEntity.ok(ack);
    }
}
