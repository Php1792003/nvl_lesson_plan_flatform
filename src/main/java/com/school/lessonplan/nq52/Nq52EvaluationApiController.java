package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52EvaluationForm;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/nq52/evaluations")
public class Nq52EvaluationApiController {

    private static final Logger log = LoggerFactory.getLogger(Nq52EvaluationApiController.class);

    private final Nq52EvaluationService evaluationService;
    private final UserRepository userRepository;

    public Nq52EvaluationApiController(Nq52EvaluationService evaluationService, UserRepository userRepository) {
        this.evaluationService = evaluationService;
        this.userRepository = userRepository;
    }

    private User getUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Người dùng không tồn tại."));
    }

    /**
     * API lưu nháp dữ liệu tự đánh giá của giáo viên (asynchronous, tránh lỗi CSRF / multipart)
     */
    @PostMapping("/{id}/save-draft")
    public ResponseEntity<Map<String, Object>> saveDraft(
            @PathVariable("id") Long id,
            @RequestBody Nq52EvaluationForm form,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = getUser(currentUser);
            Nq52Evaluation saved = evaluationService.saveTeacherEvaluation(id, form, user, false);
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("selfScoreA", saved.getSelfScoreA());
            response.put("selfScoreB", saved.getSelfScoreB());
            response.put("selfTotalScore", saved.getSelfTotalScore());
            response.put("selfClassification", saved.getSelfClassification());
            response.put("message", "Đã lưu nháp dữ liệu đánh giá thành công.");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi lưu nháp phiếu NQ52 ID {}", id, e);
            response.put("success", false);
            response.put("message", "Lỗi lưu dữ liệu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API lưu đánh giá của Tổ trưởng
     */
    @PostMapping("/{id}/save-review")
    public ResponseEntity<Map<String, Object>> saveReview(
            @PathVariable("id") Long id,
            @RequestBody Nq52EvaluationForm form,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = getUser(currentUser);
            Nq52Evaluation reviewed = evaluationService.reviewEvaluation(id, form, user);
            response.put("success", true);
            response.put("id", reviewed.getId());
            response.put("reviewerTotalScore", reviewed.getReviewerTotalScore());
            response.put("reviewerClassification", reviewed.getReviewerClassification());
            response.put("message", "Đã lưu kết quả đánh giá của Tổ trưởng thành công.");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi lưu kết quả đánh giá Tổ trưởng NQ52 ID {}", id, e);
            response.put("success", false);
            response.put("message", "Lỗi lưu đánh giá: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API lưu kết quả đánh giá của Ban Giám Hiệu / Hội đồng thi đua, khen thưởng
     */
    @PostMapping("/{id}/save-council-review")
    public ResponseEntity<Map<String, Object>> saveCouncilReview(
            @PathVariable("id") Long id,
            @RequestBody Nq52EvaluationForm form,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = getUser(currentUser);
            Nq52Evaluation reviewed = evaluationService.councilReviewEvaluation(id, form, user);
            response.put("success", true);
            response.put("id", reviewed.getId());
            response.put("bghTotalScore", reviewed.getBghTotalScore());
            response.put("bghClassification", reviewed.getBghClassification());
            response.put("message", "Đã lưu kết luận đánh giá của cơ quan có thẩm quyền thành công.");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi lưu kết quả đánh giá Hội đồng TĐ-KT NQ52 ID {}", id, e);
            response.put("success", false);
            response.put("message", "Lỗi lưu đánh giá: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

