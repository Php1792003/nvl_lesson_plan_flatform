// src/main/java/com/school/lessonplan/ai/AiReviewController.java
package com.school.lessonplan.ai;

import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.AiReviewResponse;
import com.school.lessonplan.ai.dto.ErrorResponse;
import com.school.lessonplan.ai.exception.AiResponseInvalidException;
import com.school.lessonplan.ai.exception.FileUnreadableException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/api/plans")
public class AiReviewController {

    private static final Logger log = LoggerFactory.getLogger(AiReviewController.class);

    private final AiReviewService aiReviewService;
    private final AiReviewProperties properties;

    // User rate limiter: userId -> queue of request timestamps
    private final Map<Long, ConcurrentLinkedQueue<Instant>> userRequestHistory = new ConcurrentHashMap<>();

    public AiReviewController(AiReviewService aiReviewService, AiReviewProperties properties) {
        this.aiReviewService = aiReviewService;
        this.properties = properties;
    }

    @PostMapping("/{planId}/ai-review")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EVALUATION_CREATE')")
    public ResponseEntity<?> requestAiReview(
            @PathVariable("planId") Long planId,
            @RequestParam(name = "versionId", required = false) Long versionId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        // Check user rate limit
        Long userId = (currentUser != null) ? currentUser.getId() : 0L;
        if (!checkUserRateLimit(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("RATE_LIMIT_EXCEEDED",
                            "Bạn đã thực hiện quá nhiều yêu cầu gọi AI trong 1 phút. Vui lòng chờ giây lát rồi thử lại."));
        }

        try {
            AiReviewResponse response = aiReviewService.reviewPlan(planId, versionId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Plan or version not found for AI review: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("PLAN_NOT_FOUND", e.getMessage()));

        } catch (FileUnreadableException e) {
            log.warn("File unreadable for AI review planId={}: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse(e.getCode(), e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Concurrent AI review conflict on planId={}: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("CONCURRENT_REQUEST", e.getMessage()));

        } catch (AiResponseInvalidException e) {
            log.error("AI response invalid on planId={}: {}", planId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse(e.getCode(), "Hệ thống AI phản hồi không đúng cấu trúc quy chuẩn. Vui lòng thử lại."));

        } catch (Exception e) {
            log.error("Unexpected error during AI review for planId={}: {}", planId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("AI_SERVICE_ERROR", "Không thể hoàn thành thẩm định AI: " + e.getMessage()));
        }
    }

    private boolean checkUserRateLimit(Long userId) {
        int maxPerMinute = properties.getRateLimitPerMinute();
        if (maxPerMinute <= 0) return true;

        ConcurrentLinkedQueue<Instant> queue = userRequestHistory.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minusSeconds(60);

        // Remove entries older than 1 minute
        while (!queue.isEmpty() && queue.peek().isBefore(oneMinuteAgo)) {
            queue.poll();
        }

        if (queue.size() >= maxPerMinute) {
            return false;
        }

        queue.offer(now);
        return true;
    }
}
