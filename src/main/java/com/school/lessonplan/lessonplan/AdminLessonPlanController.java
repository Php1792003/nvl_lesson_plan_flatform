package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.BusinessException;
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

    public AdminLessonPlanController(LessonPlanService lessonPlanService,
                                    EvaluationService evaluationService,
                                    DepartmentRepository departmentRepository) {
        this.lessonPlanService = lessonPlanService;
        this.evaluationService = evaluationService;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String listAllPlans(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "schoolYear", required = false) String schoolYear,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Page<LessonPlan> plansPage = lessonPlanService.searchAllForAdmin(
            search, departmentId, schoolYear, semester,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("plansPage", plansPage);
        model.addAttribute("search", search);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("schoolYear", schoolYear);
        model.addAttribute("semester", semester);
        model.addAttribute("departments", departmentRepository.findAll());

        return "admin/plans/list";
    }

    @PostMapping("/{id}/evaluations")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EVALUATION_CREATE')")
    public String addEvaluation(
            @PathVariable("id") Long id,
            @RequestParam("versionId") Long versionId,
            @RequestParam("content") String content,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

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

        try {
            evaluationService.editEvaluation(evaluationId, currentUser, content);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật nhận xét thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/plans/" + id;
    }
}
