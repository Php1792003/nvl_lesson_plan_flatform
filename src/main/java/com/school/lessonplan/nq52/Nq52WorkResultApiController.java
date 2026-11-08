package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
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
@RequestMapping("/api/nq52/work-results")
public class Nq52WorkResultApiController {

    private static final Logger log = LoggerFactory.getLogger(Nq52WorkResultApiController.class);

    private final Nq52WorkResultService workResultService;
    private final UserRepository userRepository;

    public Nq52WorkResultApiController(Nq52WorkResultService workResultService, UserRepository userRepository) {
        this.workResultService = workResultService;
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
     * API lưu nháp dữ liệu tự đánh giá kết quả (Mẫu 01TKNV) của giáo viên trước khi
     * ký số
     */
    @PostMapping("/{id}/save-draft")
    public ResponseEntity<Map<String, Object>> saveDraft(
            @PathVariable("id") Long id,
            @RequestBody Nq52WorkResultForm form,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = getUser(currentUser);
            Nq52WorkResult saved = workResultService.saveWorkResult(id, form, user, false);
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("totalAssigned", saved.getTotalAssigned());
            response.put("totalCompleted", saved.getTotalCompleted());
            response.put("completionRate", saved.getCompletionRate());
            response.put("message", "Đã lưu dữ liệu tự đánh giá kết quả vào cơ sở dữ liệu thành công.");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi lưu nháp bảng kết quả NQ52 ID {}", id, e);
            response.put("success", false);
            response.put("message", "Lỗi lưu dữ liệu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API lưu ý kiến đánh giá của Tổ trưởng trước khi ký duyệt
     */
    @PostMapping("/{id}/save-review")
    public ResponseEntity<Map<String, Object>> saveReview(
            @PathVariable("id") Long id,
            @RequestBody Nq52WorkResultForm form,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = getUser(currentUser);
            String notes = form.getReviewerNotes();
            if (notes == null || notes.isBlank()) {
                notes = form.getBghNotes();
            }
            Nq52WorkResult saved = workResultService.saveReviewNotes(id, notes, user);
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("message", "Đã lưu ý kiến đánh giá thành công.");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi lưu ý kiến Tổ trưởng cho bảng kết quả NQ52 ID {}", id, e);
            response.put("success", false);
            response.put("message", "Lỗi lưu đánh giá: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API lưu ý kiến kết luận của Ban Giám Hiệu trước khi ký duyệt chính thức
     */
    @PostMapping("/{id}/save-council-review")
    public ResponseEntity<Map<String, Object>> saveCouncilReview(
            @PathVariable("id") Long id,
            @RequestBody Nq52WorkResultForm form,
            @AuthenticationPrincipal CurrentUser currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = getUser(currentUser);
            String notes = form.getBghNotes() != null && !form.getBghNotes().isBlank()
                    ? form.getBghNotes() : form.getReviewerNotes();
            Nq52WorkResult saved = workResultService.saveReviewNotes(id, notes, user);
            response.put("success", true);
            response.put("id", saved.getId());
            response.put("message", "Đã lưu ý kiến nhận xét của Ban Giám Hiệu thành công.");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (Exception e) {
            log.error("Lỗi khi lưu ý kiến Ban Giám Hiệu cho bảng kết quả NQ52 ID {}", id, e);
            response.put("success", false);
            response.put("message", "Lỗi lưu nhận xét Ban Giám Hiệu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
