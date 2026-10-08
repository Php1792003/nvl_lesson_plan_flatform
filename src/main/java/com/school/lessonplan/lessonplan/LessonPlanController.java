package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.Evaluation;
import com.school.lessonplan.evaluation.EvaluationService;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.share.LessonPlanShare;
import com.school.lessonplan.share.ShareService;
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
    public String listPlans(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(name = "tab", defaultValue = "my") String tab,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        if ("shared".equals(tab)) {
            PageRequest sharePageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "grantedAt"));
            Page<LessonPlanShare> sharedPage = shareService.getSharedWithMe(currentUser, sharePageRequest);
            model.addAttribute("sharedPage", sharedPage);
        } else {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
            Page<LessonPlan> plansPage = lessonPlanService.getAccessiblePlans(currentUser, pageRequest);
            model.addAttribute("plansPage", plansPage);
        }

        model.addAttribute("currentTab", tab);
        return "plans/list";
    }

    @GetMapping("/new")
    public String newPlanForm(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "plans/create";
    }

    @PostMapping
    public String createPlan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("title") String title,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("grade") String grade,
            @RequestParam("schoolYear") String schoolYear,
            @RequestParam("semester") Integer semester,
            @RequestParam(name = "weekNo", required = false) Integer weekNo,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            LessonPlan plan = lessonPlanService.createPlan(
                currentUser, title, departmentId, grade, schoolYear, semester, weekNo, description, file);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo giáo án thành công!");
            return "redirect:/plans/" + plan.getId();
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/plans/new";
        }
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

        return "plans/detail";
    }

    @GetMapping("/{id}/edit")
    public String editPlanForm(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            Model model) {

        LessonPlan plan = lessonPlanService.getPlanById(id, currentUser);
        accessService.requireManageAccess(currentUser, plan);

        model.addAttribute("plan", plan);
        model.addAttribute("departments", departmentRepository.findAll());
        return "plans/edit";
    }

    @PostMapping("/{id}")
    public String updatePlan(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("title") String title,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("grade") String grade,
            @RequestParam("schoolYear") String schoolYear,
            @RequestParam("semester") Integer semester,
            @RequestParam(name = "weekNo", required = false) Integer weekNo,
            @RequestParam(name = "description", required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            lessonPlanService.updateMetadata(
                id, currentUser, title, departmentId, grade, schoolYear, semester, weekNo, description);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin giáo án thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/plans/" + id;
    }

    @PostMapping("/{id}/versions")
    public String uploadNewVersion(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

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

        try {
            evaluationService.replyToEvaluation(evaluationId, currentUser, content);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi phản hồi nhận xét thành công!");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/plans/" + id;
    }
}
