package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.EvaluationService;
import com.school.lessonplan.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/plans")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PLAN_VIEW_ALL')")
public class AdminLessonPlanController {

    private final LessonPlanService lessonPlanService;
    private final EvaluationService evaluationService;
    private final DepartmentRepository departmentRepository;
    private final com.school.lessonplan.ai.AiReviewService aiReviewService;

    public AdminLessonPlanController(LessonPlanService lessonPlanService,
                                    EvaluationService evaluationService,
                                    DepartmentRepository departmentRepository,
                                    com.school.lessonplan.ai.AiReviewService aiReviewService) {
        this.lessonPlanService = lessonPlanService;
        this.evaluationService = evaluationService;
        this.departmentRepository = departmentRepository;
        this.aiReviewService = aiReviewService;
    }

    @GetMapping
    public String listAllPlans(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "schoolYear", required = false) String schoolYear,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "favorite", required = false) Boolean favorite,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanSchoolYear = (schoolYear != null && !schoolYear.trim().isEmpty()) ? schoolYear.trim() : null;

        Page<LessonPlan> plansPage = lessonPlanService.searchAllForAdmin(
            cleanSearch, departmentId, cleanSchoolYear, semester, favorite,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("plansPage", plansPage);
        model.addAttribute("search", cleanSearch);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("schoolYear", cleanSchoolYear);
        model.addAttribute("semester", semester);
        model.addAttribute("favorite", favorite);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("schoolYears", lessonPlanService.getDistinctSchoolYears());

        return "admin/plans/admin-plan-catalog";
    }

    /**
     * Endpoint Ban Giám Hiệu dọn dẹp giáo án sau mỗi học kỳ.
     * Tự động bảo vệ tất cả giáo án được đánh dấu yêu thích.
     */
    @PostMapping("/cleanup-semester")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PLAN_VIEW_ALL')")
    public String cleanupSemester(
            @RequestParam("schoolYear") String schoolYear,
            @RequestParam("semester") int semester,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            LessonPlanService.SemesterCleanupResult result =
                lessonPlanService.cleanupSemesterPlans(schoolYear, semester, currentUser);

            redirectAttributes.addFlashAttribute("successMessage",
                String.format("Dọn dẹp thành công học kỳ %d (%s)! Đã xoá %d giáo án thông thường. Đã bảo vệ an toàn %d giáo án yêu thích (không bị xoá) và %d giáo án có nhận xét.",
                    semester, schoolYear, result.deletedCount(), result.protectedFavoriteCount(), result.protectedEvaluatedCount()));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/plans?schoolYear=" + (schoolYear != null ? schoolYear : "") + "&semester=" + semester;
    }

    @PostMapping("/{id}/evaluations")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EVALUATION_CREATE')")
    public String addEvaluation(
            @PathVariable("id") Long id,
            @RequestParam("versionId") Long versionId,
            @RequestParam("content") String content,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung nhận xét không được để trống.");
            return "redirect:/plans/" + id;
        }

        try {
            evaluationService.addEvaluation(id, versionId, currentUser, content);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi nhận xét thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/evaluations/{eid}/edit")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EVALUATION_CREATE')")
    public String editEvaluation(
            @PathVariable("id") Long id,
            @PathVariable("eid") Long evaluationId,
            @RequestParam("content") String content,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung nhận xét không được để trống.");
            return "redirect:/plans/" + id;
        }

        try {
            evaluationService.editEvaluation(evaluationId, currentUser, content);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật nhận xét thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/evaluations/{eid}/delete")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EVALUATION_CREATE')")
    public String deleteEvaluation(
            @PathVariable("id") Long id,
            @PathVariable("eid") Long evaluationId,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            evaluationService.deleteEvaluation(evaluationId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thu hồi nhận xét thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/plans/" + id;
    }

    @GetMapping("/{id}/ai-review")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EVALUATION_CREATE')")
    @ResponseBody
    public org.springframework.http.ResponseEntity<com.school.lessonplan.ai.dto.AiReviewResponse> getAiReview(
            @PathVariable("id") Long id,
            @RequestParam(name = "versionId", required = false) Long versionId) {
        try {
            com.school.lessonplan.ai.dto.AiReviewResponse review = aiReviewService.reviewPlan(id, versionId);
            return org.springframework.http.ResponseEntity.ok(review);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                com.school.lessonplan.ai.dto.AiReviewResponse.error("Không thể tạo nhận xét AI: " + e.getMessage())
            );
        }
    }
}

