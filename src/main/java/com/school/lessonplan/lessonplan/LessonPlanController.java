package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.Evaluation;
import com.school.lessonplan.evaluation.EvaluationService;
import com.school.lessonplan.lessonplan.dto.PlanCreateForm;
import com.school.lessonplan.lessonplan.dto.PlanUpdateForm;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.share.LessonPlanShare;
import com.school.lessonplan.share.ShareService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/plans")
public class LessonPlanController {

    private final LessonPlanService lessonPlanService;
    private final LessonPlanAccessService accessService;
    private final EvaluationService evaluationService;
    private final ShareService shareService;
    private final DepartmentRepository departmentRepository;

    public LessonPlanController(LessonPlanService lessonPlanService,
                                LessonPlanAccessService accessService,
                                EvaluationService evaluationService,
                                ShareService shareService,
                                DepartmentRepository departmentRepository) {
        this.lessonPlanService = lessonPlanService;
        this.accessService = accessService;
        this.evaluationService = evaluationService;
        this.shareService = shareService;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String myPlans(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(name = "tab", defaultValue = "my") String tab,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if ("shared".equals(tab)) {
            Page<LessonPlanShare> sharedPage = shareService.getSharedWithMe(currentUser, pageRequest);
            model.addAttribute("sharedPage", sharedPage);
        } else if ("orphan".equals(tab)) {
            boolean canViewOrphan = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));
            if (canViewOrphan) {
                Page<LessonPlan> orphanPage = lessonPlanService.getOrphanPlans(pageRequest);
                model.addAttribute("orphanPage", orphanPage);
            }
        } else {
            Page<LessonPlan> plansPage = lessonPlanService.getAccessiblePlans(currentUser, pageRequest);
            model.addAttribute("plansPage", plansPage);
        }

        model.addAttribute("currentTab", tab);
        return "plans/plan-catalog";
    }

    @GetMapping("/new")
    public String newPlanForm(Model model) {
        if (!model.containsAttribute("planForm")) {
            PlanCreateForm form = new PlanCreateForm();
            form.setSchoolYear("2025-2026");
            form.setSemester(1);
            form.setGrade("6");
            model.addAttribute("planForm", form);
        }
        model.addAttribute("departments", departmentRepository.findAll());
        return "plans/plan-builder";
    }

    @PostMapping
    public String createPlan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @ModelAttribute("planForm") PlanCreateForm planForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (planForm.getFile() == null || planForm.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "Empty", "Vui lòng chọn file tải lên.");
        }

        if (!bindingResult.hasErrors()) {
            try {
                LessonPlan plan = lessonPlanService.createPlan(
                    currentUser,
                    planForm.getTitle(),
                    planForm.getDepartmentId(),
                    planForm.getGrade(),
                    planForm.getSchoolYear(),
                    planForm.getSemester(),
                    planForm.getWeekNo(),
                    planForm.getDescription(),
                    planForm.getFile()
                );
                redirectAttributes.addFlashAttribute("successMessage", "Tạo giáo án thành công!");
                return "redirect:/plans/" + plan.getId();
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        model.addAttribute("departments", departmentRepository.findAll());
        return "plans/plan-builder";
    }

    @GetMapping("/{id}")
    public String viewPlan(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            Model model) {

        LessonPlan plan = lessonPlanService.getPlanById(id, currentUser);
        List<LessonPlanVersion> versions = lessonPlanService.getVersions(id);
        List<Evaluation> evaluations = evaluationService.getEvaluationsForPlan(id, currentUser);

        boolean isOwner = plan.getOwner() != null && plan.getOwner().getId().equals(currentUser.getId());
        List<LessonPlanShare> activeShares = isOwner ? shareService.getActiveShares(id, currentUser) : List.of();

        boolean isAdmin = currentUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));

        model.addAttribute("plan", plan);
        model.addAttribute("versions", versions);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("activeShares", activeShares);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("canManage", accessService.canManage(currentUser, plan));

        return "plans/plan-overview";
    }

    @GetMapping("/{id}/edit")
    public String editPlanForm(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            Model model) {

        LessonPlan plan = lessonPlanService.getPlanById(id, currentUser);
        accessService.requireManageAccess(currentUser, plan);

        if (!model.containsAttribute("planForm")) {
            PlanUpdateForm form = new PlanUpdateForm();
            form.setTitle(plan.getTitle());
            if (plan.getDepartment() != null) form.setDepartmentId(plan.getDepartment().getId());
            form.setGrade(plan.getGrade());
            form.setSchoolYear(plan.getSchoolYear());
            form.setSemester(plan.getSemester());
            form.setWeekNo(plan.getWeekNo());
            form.setDescription(plan.getDescription());
            model.addAttribute("planForm", form);
        }

        model.addAttribute("plan", plan);
        model.addAttribute("departments", departmentRepository.findAll());
        return "plans/plan-editor";
    }

    @PostMapping("/{id}")
    public String updatePlan(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @ModelAttribute("planForm") PlanUpdateForm planForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            try {
                lessonPlanService.updateMetadata(
                    id, currentUser, planForm.getTitle(), planForm.getDepartmentId(),
                    planForm.getGrade(), planForm.getSchoolYear(), planForm.getSemester(),
                    planForm.getWeekNo(), planForm.getDescription());
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin giáo án thành công!");
                return "redirect:/plans/" + id;
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), e.getCode(), e.getMessage());
            } catch (BusinessException e) {
                bindingResult.reject("form.error", e.getMessage());
            }
        }

        LessonPlan plan = lessonPlanService.getPlanById(id, currentUser);
        model.addAttribute("plan", plan);
        model.addAttribute("departments", departmentRepository.findAll());
        return "plans/plan-editor";
    }

    @PostMapping("/{id}/versions")
    public String uploadNewVersion(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file tải lên.");
            return "redirect:/plans/" + id;
        }

        try {
            LessonPlanVersion version = lessonPlanService.addVersion(id, currentUser, file);
            redirectAttributes.addFlashAttribute("successMessage",
                "Tải lên phiên bản " + version.getVersionNo() + " thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deletePlan(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            lessonPlanService.deletePlan(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xoá giáo án thành công!");
            return "redirect:/plans";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/plans/" + id;
        }
    }

    @GetMapping("/{id}/versions/{vid}/download")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable("id") Long id,
            @PathVariable("vid") Long versionId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        LessonPlanService.DownloadResult result = lessonPlanService.downloadVersion(id, versionId, currentUser);

        String encodedFilename = URLEncoder.encode(result.originalFilename(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
            .header("X-Content-Type-Options", "nosniff")
            .body(result.resource());
    }

    @GetMapping("/{id}/versions/{vid}/preview")
    public ResponseEntity<Resource> previewVersion(
            @PathVariable("id") Long id,
            @PathVariable("vid") Long versionId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        LessonPlanService.DownloadResult result = lessonPlanService.previewVersion(id, versionId, currentUser);

        String encodedFilename = URLEncoder.encode(result.originalFilename(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(result.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
            .header("X-Content-Type-Options", "nosniff")
            .body(result.resource());
    }

    @PostMapping("/{id}/shares")
    public String sharePlan(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("email") String email,
            @RequestParam(name = "expireDays", required = false) Integer expireDays,
            RedirectAttributes redirectAttributes) {

        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập email giáo viên nhận chia sẻ.");
            return "redirect:/plans/" + id;
        }

        try {
            shareService.sharePlan(id, currentUser, email, expireDays);
            redirectAttributes.addFlashAttribute("successMessage", "Chia sẻ giáo án thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/shares/{shareId}/revoke")
    public String revokeShare(
            @PathVariable("id") Long id,
            @PathVariable("shareId") Long shareId,
            @AuthenticationPrincipal CurrentUser currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            shareService.revokeShare(id, shareId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Thu hồi quyền chia sẻ thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/evaluations/{eid}/replies")
    public String replyEvaluation(
            @PathVariable("id") Long id,
            @PathVariable("eid") Long evaluationId,
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("content") String content,
            RedirectAttributes redirectAttributes) {

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung phản hồi không được để trống.");
            return "redirect:/plans/" + id;
        }

        try {
            evaluationService.replyToEvaluation(evaluationId, currentUser, content);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi phản hồi nhận xét thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/plans/" + id;
    }
}
