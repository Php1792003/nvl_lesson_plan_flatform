package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentGroupService;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.EvaluationService;
import com.school.lessonplan.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/plans")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PLAN_VIEW_ALL')")
public class AdminLessonPlanController {

    private static final Logger log = LoggerFactory.getLogger(AdminLessonPlanController.class);

    private final LessonPlanService lessonPlanService;
    private final EvaluationService evaluationService;
    private final DepartmentRepository departmentRepository;
    private final com.school.lessonplan.ai.AiReviewService aiReviewService;
    private final DepartmentGroupService departmentGroupService;

    public AdminLessonPlanController(LessonPlanService lessonPlanService,
                                    EvaluationService evaluationService,
                                    DepartmentRepository departmentRepository,
                                    com.school.lessonplan.ai.AiReviewService aiReviewService,
                                    DepartmentGroupService departmentGroupService) {
        this.lessonPlanService = lessonPlanService;
        this.evaluationService = evaluationService;
        this.departmentRepository = departmentRepository;
        this.aiReviewService = aiReviewService;
        this.departmentGroupService = departmentGroupService;
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
            @AuthenticationPrincipal CurrentUser currentUser,
            Model model) {

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanSchoolYear = (schoolYear != null && !schoolYear.trim().isEmpty()) ? schoolYear.trim() : null;

        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isDeptHead = currentUser != null && departmentGroupService.isDepartmentHead(currentUser);
        String groupName = currentUser != null ? departmentGroupService.getDepartmentGroupName(currentUser) : "";

        List<Department> accessibleDepartments = departmentGroupService.getAccessibleDepartments(currentUser);
        List<Long> accessibleDeptIds = departmentGroupService.getAccessibleDepartmentIds(currentUser);

        log.info("[AdminPlans] user={}, isAdmin={}, isDeptHead={}, groupName={}, deptName={}, accessibleDeptIds={}, accessibleDepts={}",
                currentUser != null ? currentUser.getUsername() : "null",
                isAdmin, isDeptHead, groupName,
                currentUser != null ? currentUser.getDepartmentName() : "null",
                accessibleDeptIds,
                accessibleDepartments.stream().map(d -> d.getId() + ":" + d.getName()).collect(java.util.stream.Collectors.joining(", ")));

        // Nếu là tổ trưởng và người dùng chọn bộ môn: chỉ chấp nhận ID thuộc tổ chuyên môn của mình
        Long effectiveDeptId = departmentId;
        if (isDeptHead && accessibleDeptIds != null && effectiveDeptId != null && !accessibleDeptIds.contains(effectiveDeptId)) {
            effectiveDeptId = null;
        }

        Page<LessonPlan> plansPage = lessonPlanService.searchAllForAdmin(
            cleanSearch, effectiveDeptId, cleanSchoolYear, semester, favorite, currentUser,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("plansPage", plansPage);
        model.addAttribute("search", cleanSearch);
        model.addAttribute("departmentId", effectiveDeptId);
        model.addAttribute("schoolYear", cleanSchoolYear);
        model.addAttribute("semester", semester);
        model.addAttribute("favorite", favorite);
        model.addAttribute("departments", accessibleDepartments);
        model.addAttribute("schoolYears", lessonPlanService.getDistinctSchoolYears());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isDeptHead", isDeptHead);
        model.addAttribute("groupName", groupName);

        return "admin/plans/admin-plan-catalog";
    }

    /**
     * Endpoint Ban Giám Hiệu dọn dẹp giáo án sau mỗi học kỳ.
     * Tự động bảo vệ tất cả giáo án được đánh dấu yêu thích.
     */
    @PostMapping("/cleanup-semester")
    @PreAuthorize("hasRole('ADMIN')")
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
            @RequestParam(name = "versionId", required = false) Long versionId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        try {
            com.school.lessonplan.ai.dto.AiReviewResponse review = aiReviewService.reviewPlan(id, versionId, currentUser);
            return org.springframework.http.ResponseEntity.ok(review);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                com.school.lessonplan.ai.dto.AiReviewResponse.error("Không thể tạo nhận xét AI: " + e.getMessage())
            );
        }
    }
}

