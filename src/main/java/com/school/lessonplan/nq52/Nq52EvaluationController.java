package com.school.lessonplan.nq52;

import com.school.lessonplan.campus.Campus;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.nq52.dto.Nq52EvaluationForm;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/nq52")
public class Nq52EvaluationController {

    private final Nq52EvaluationService evaluationService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final com.school.lessonplan.nq52.signature.Nq52SignatureService signatureService;

    public Nq52EvaluationController(Nq52EvaluationService evaluationService,
                                    UserRepository userRepository,
                                    DepartmentRepository departmentRepository,
                                    CampusRepository campusRepository,
                                    com.school.lessonplan.nq52.signature.Nq52SignatureService signatureService) {
        this.evaluationService = evaluationService;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.signatureService = signatureService;
    }

    /**
     * Màn hình danh sách phiếu đánh giá của cá nhân giáo viên
     */
    @GetMapping({"", "/evaluations"})
    public String myEvaluations(@AuthenticationPrincipal CurrentUser currentUser,
                                @RequestParam(name = "month", required = false) Integer month,
                                @RequestParam(name = "year", required = false) Integer year,
                                Model model) {
        User user = getUser(currentUser);
        List<Nq52Evaluation> evaluations = evaluationService.getMyEvaluations(user.getId(), month, year);

        List<Nq52Evaluation> allMy = evaluationService.getMyEvaluations(user.getId());
        long totalCount = allMy.size();
        long draftCount = allMy.stream().filter(e -> e.getStatus() == Nq52Status.DRAFT).count();
        long submittedCount = allMy.stream().filter(e -> e.getStatus() == Nq52Status.SUBMITTED).count();
        long reviewedCount = allMy.stream().filter(e -> e.getStatus() == Nq52Status.REVIEWED || e.getStatus() == Nq52Status.COUNCIL_REVIEWED).count();

        int currentYear = LocalDate.now().getYear();
        List<Integer> years = List.of(currentYear + 1, currentYear, currentYear - 1, currentYear - 2);

        model.addAttribute("pageTitle", "Đánh giá Nghị quyết 52");
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("availableYears", years);
        model.addAttribute("isReviewer", evaluationService.isReviewer(user));
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("submittedCount", submittedCount);
        model.addAttribute("reviewedCount", reviewedCount);

        return "nq52/evaluation-list";
    }

    /**
     * Tạo hoặc mở phiếu đánh giá theo tháng và năm
     */
    @GetMapping("/new")
    public String createOrOpen(@AuthenticationPrincipal CurrentUser currentUser,
                               @RequestParam(name = "month", required = false) Integer month,
                               @RequestParam(name = "year", required = false) Integer year) {
        User user = getUser(currentUser);
        int m = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();
        int y = (year != null && year >= 2020 && year <= 2050) ? year : LocalDate.now().getYear();

        Nq52Evaluation eval = evaluationService.getOrCreateEvaluation(user, m, y);
        return "redirect:/nq52/evaluations/" + eval.getId();
    }

    /**
     * Xem / Soạn thảo phiếu đánh giá
     */
    @GetMapping("/evaluations/{id}")
    public String viewOrEdit(@PathVariable("id") Long id,
                             @AuthenticationPrincipal CurrentUser currentUser,
                             Model model) {
        User user = getUser(currentUser);
        Nq52Evaluation eval = evaluationService.getEvaluationForView(id, user);

        boolean isOwner = eval.getUser().getId().equals(user.getId());
        boolean isAdmin = evaluationService.isAdmin(user);
        boolean isDeptLeader = evaluationService.isDeptLeaderOf(user, eval);

        // Cấp 1: Giáo viên tự chấm (Chủ sở hữu khi phiếu đang ở trạng thái DRAFT)
        boolean canEdit = isOwner && eval.getStatus() == Nq52Status.DRAFT;

        // Cấp 2: Tổ trưởng đánh giá (Tổ trưởng phụ trách khi phiếu đã nộp SUBMITTED hoặc REVIEWED)
        boolean canReview = !isAdmin && evaluationService.canReview(user, eval) && eval.getStatus() != Nq52Status.DRAFT;

        // Cấp 3: Ban Giám Hiệu đánh giá chính thức của Hội đồng thi đua, khen thưởng
        // Ban Giám Hiệu là cấp duyệt cuối cùng, chỉ duyệt khi Tổ trưởng đã hoàn tất thẩm định (REVIEWED hoặc COUNCIL_REVIEWED)
        boolean canBghReview = isAdmin && (eval.getStatus() == Nq52Status.REVIEWED || eval.getStatus() == Nq52Status.COUNCIL_REVIEWED);

        Nq52EvaluationForm form = evaluationService.populateForm(eval);

        model.addAttribute("pageTitle", "Phiếu đánh giá NQ 52 - Tháng " + eval.getEvaluationMonth() + "/" + eval.getEvaluationYear());
        model.addAttribute("eval", eval);
        model.addAttribute("form", form);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("canReview", canReview);
        model.addAttribute("canBghReview", canBghReview);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isDeptLeader", isDeptLeader);
        model.addAttribute("isReviewer", evaluationService.isReviewer(user));

        List<com.school.lessonplan.nq52.signature.dto.SignatureItemDto> signatures = signatureService.getSignaturesForDocument("EVALUATION", id);
        model.addAttribute("signatures", signatures);
        model.addAttribute("sigTeacher", getSigByRole(signatures, "TEACHER"));
        model.addAttribute("sigDeptLeader", getSigByRole(signatures, "DEPT_LEADER"));
        model.addAttribute("sigPrincipal", getSigByRole(signatures, "PRINCIPAL"));
        model.addAttribute("docType", "EVALUATION");
        model.addAttribute("docId", id);

        return "nq52/evaluation-form";
    }

    /**
     * Giáo viên lưu nháp, nộp phiếu hoặc xuất file Word
     */
    @PostMapping("/evaluations/{id}/save")
    public String saveTeacherEvaluation(@PathVariable("id") Long id,
                                        @ModelAttribute("form") Nq52EvaluationForm form,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        RedirectAttributes redirectAttributes) {
        User user = getUser(currentUser);
        boolean isSubmit = "SUBMIT".equalsIgnoreCase(form.getAction());
        boolean isExport = "EXPORT".equalsIgnoreCase(form.getAction()) || "SAVE_AND_EXPORT".equalsIgnoreCase(form.getAction());

        try {
            Nq52Evaluation saved = evaluationService.saveTeacherEvaluation(id, form, user, isSubmit);
            if (isExport) {
                return "redirect:/nq52/evaluations/" + id + "/export";
            }
            if (isSubmit) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã nộp phiếu đánh giá Tháng " + saved.getEvaluationMonth() + "/" + saved.getEvaluationYear() + " đến Tổ trưởng thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã lưu bản nháp phiếu đánh giá thành công!");
            }
        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi lưu phiếu: " + e.getMessage());
        }

        return "redirect:/nq52/evaluations/" + id;
    }

    /**
     * Xuất file Word Mẫu 03 (.docx) và lưu lại vào database, đồng thời tải xuống máy
     */
    @GetMapping("/evaluations/{id}/export")
    public ResponseEntity<byte[]> exportAndDownload(@PathVariable("id") Long id,
                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        Nq52Evaluation eval = evaluationService.exportAndSaveDocx(id, user);
        byte[] data = eval.getExportedFileData();

        String filename = eval.getExportedFileName() != null ? eval.getExportedFileName() : "Phieu_Danh_Gia_NQ52.docx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(data.length)
                .body(data);
    }

    /**
     * Tải file Word đã lưu trước đó trong cơ sở dữ liệu
     */
    @GetMapping("/evaluations/{id}/download")
    public ResponseEntity<byte[]> downloadSavedFile(@PathVariable("id") Long id,
                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        Nq52Evaluation eval = evaluationService.getEvaluationForView(id, user);
        byte[] data = evaluationService.getExportedFileData(id, user);

        String filename = eval.getExportedFileName() != null ? eval.getExportedFileName() : "Phieu_Danh_Gia_NQ52.docx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(data.length)
                .body(data);
    }

    /**
     * Tổ trưởng / Ban Giám Hiệu chấm điểm và duyệt phiếu
     */
    @PostMapping("/evaluations/{id}/review")
    public String reviewEvaluation(@PathVariable("id") Long id,
                                   @ModelAttribute("form") Nq52EvaluationForm form,
                                   @AuthenticationPrincipal CurrentUser currentUser,
                                   RedirectAttributes redirectAttributes) {
        User reviewer = getUser(currentUser);

        try {
            Nq52Evaluation reviewed = evaluationService.reviewEvaluation(id, form, reviewer);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã hoàn tất đánh giá phiếu của giáo viên " + reviewed.getTeacherNameSnapshot() + " (Điểm: " + reviewed.getReviewerTotalScore() + " - " + reviewed.getReviewerClassification() + ")!");
        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/nq52/evaluations/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi đánh giá phiếu: " + e.getMessage());
            return "redirect:/nq52/evaluations/" + id;
        }

        return "redirect:/nq52/reviews";
    }

    /**
     * Ban Giám Hiệu chấm điểm và hoàn tất đánh giá của Hội đồng thi đua, khen thưởng (Cấp 3)
     */
    @PostMapping("/evaluations/{id}/council-review")
    public String councilReviewEvaluation(@PathVariable("id") Long id,
                                          @ModelAttribute("form") Nq52EvaluationForm form,
                                          @AuthenticationPrincipal CurrentUser currentUser,
                                          RedirectAttributes redirectAttributes) {
        User bghAdmin = getUser(currentUser);

        try {
            Nq52Evaluation reviewed = evaluationService.councilReviewEvaluation(id, form, bghAdmin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ban Giám Hiệu đã hoàn tất đánh giá của Hội đồng thi đua, khen thưởng cho giáo viên " + reviewed.getTeacherNameSnapshot() + " (Điểm chính thức: " + reviewed.getBghTotalScore() + " - " + reviewed.getBghClassification() + ")!");
        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/nq52/evaluations/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi Hội đồng đánh giá phiếu: " + e.getMessage());
            return "redirect:/nq52/evaluations/" + id;
        }

        return "redirect:/nq52/reviews";
    }

    /**
     * Danh sách phiếu cần duyệt dành cho Tổ trưởng chuyên môn & Ban Giám Hiệu
     * - BGH: Có toàn quyền xem và chuyển đổi giữa 3 Điểm trường / Phân hiệu (hoặc xem toàn trường).
     * - Tổ trưởng: Cố định phạm vi xét duyệt tại đúng Điểm trường / Phân hiệu và Tổ môn được phân công.
     */
    @GetMapping("/reviews")
    public String reviewQueue(@AuthenticationPrincipal CurrentUser currentUser,
                              @RequestParam(name = "campusId", required = false) Long campusId,
                              @RequestParam(name = "deptId", required = false) Long deptId,
                              @RequestParam(name = "status", required = false) String statusStr,
                              @RequestParam(name = "month", required = false) Integer month,
                              @RequestParam(name = "year", required = false) Integer year,
                              Model model) {
        User user = getUser(currentUser);
        if (!evaluationService.isReviewer(user)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập khu vực duyệt phiếu của Tổ trưởng.");
        }

        boolean isAdmin = evaluationService.isAdmin(user);

        // Nếu không phải Ban Giám Hiệu (là Tổ trưởng): Cố định chặt chẽ theo Campus và Department của Tổ trưởng
        if (!isAdmin) {
            campusId = (user.getCampus() != null) ? user.getCampus().getId() : null;
            deptId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;
        }

        // Mặc định ở site duyệt phiếu chỉ hiển thị danh sách CẦN DUYỆT:
        // - Ban Giám Hiệu duyệt cuối cùng: chỉ hiển thị các đánh giá cần duyệt khi Tổ trưởng đã duyệt (REVIEWED)
        // - Tổ trưởng: Cần duyệt là các phiếu giáo viên đã nộp (SUBMITTED)
        String defaultStatus = isAdmin ? "REVIEWED" : "SUBMITTED";
        String selectedStatus = (statusStr != null && !statusStr.isBlank()) ? statusStr.toUpperCase() : defaultStatus;

        Nq52Status filterStatus = null;
        if ("SUBMITTED".equals(selectedStatus)) {
            filterStatus = Nq52Status.SUBMITTED;
        } else if ("REVIEWED".equals(selectedStatus)) {
            filterStatus = Nq52Status.REVIEWED;
        } else if ("COUNCIL_REVIEWED".equals(selectedStatus)) {
            filterStatus = Nq52Status.COUNCIL_REVIEWED;
        }

        List<Nq52Evaluation> evaluations = evaluationService.getDepartmentEvaluations(user, campusId, deptId, filterStatus, month, year);
        // Khu vực duyệt phiếu chỉ quản lý hồ sơ đã nộp, loại bỏ hoàn toàn DRAFT
        evaluations = evaluations.stream()
                .filter(e -> e.getStatus() != Nq52Status.DRAFT)
                .toList();

        List<Department> departments = departmentRepository.findAll();
        List<Campus> campuses = campusRepository.findAll();

        List<Nq52Evaluation> allDept = evaluationService.getDepartmentEvaluations(user, campusId, deptId, null, null, null);
        long councilReviewedCount = allDept.stream().filter(e -> e.getStatus() == Nq52Status.COUNCIL_REVIEWED).count();
        long draftReviewCount = allDept.stream().filter(e -> e.getStatus() == Nq52Status.DRAFT).count();

        long pendingReviewCount;
        long completedReviewCount;
        long totalSubmittedCount;

        if (isAdmin) {
            // Đối với Ban Giám Hiệu:
            // - Cần duyệt: Các đánh giá Tổ trưởng đã duyệt xong (REVIEWED)
            // - Đã đánh giá / Hoàn thành: Các đánh giá Ban Giám Hiệu đã duyệt chính thức (COUNCIL_REVIEWED)
            // - Tổng hồ sơ tiếp nhận: Hồ sơ đã qua cấp Tổ (REVIEWED + COUNCIL_REVIEWED)
            pendingReviewCount = allDept.stream().filter(e -> e.getStatus() == Nq52Status.REVIEWED).count();
            completedReviewCount = councilReviewedCount;
            totalSubmittedCount = pendingReviewCount + completedReviewCount;
        } else {
            // Đối với Tổ trưởng:
            // - Cần duyệt: Các phiếu giáo viên đã nộp chờ Tổ trưởng duyệt (SUBMITTED)
            // - Đã đánh giá: Các phiếu Tổ trưởng đã hoàn tất (REVIEWED hoặc COUNCIL_REVIEWED)
            // - Tổng hồ sơ: SUBMITTED + REVIEWED + COUNCIL_REVIEWED
            pendingReviewCount = allDept.stream().filter(e -> e.getStatus() == Nq52Status.SUBMITTED).count();
            completedReviewCount = allDept.stream().filter(e -> e.getStatus() == Nq52Status.REVIEWED || e.getStatus() == Nq52Status.COUNCIL_REVIEWED).count();
            totalSubmittedCount = pendingReviewCount + completedReviewCount;
        }

        // Xác định Điểm trường / Phân hiệu đang xem
        Campus currentViewingCampus = null;
        if (campusId != null) {
            final Long cid = campusId;
            currentViewingCampus = campuses.stream().filter(c -> c.getId().equals(cid)).findFirst().orElse(null);
        } else if (!isAdmin && user.getCampus() != null) {
            currentViewingCampus = user.getCampus();
        }

        int currentYear = LocalDate.now().getYear();
        List<Integer> years = List.of(currentYear + 1, currentYear, currentYear - 1, currentYear - 2);

        model.addAttribute("pageTitle", isAdmin ? "Ban Giám Hiệu duyệt đánh giá NQ 52" : "Duyệt phiếu đánh giá NQ 52 cấp Tổ");
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("departments", departments);
        model.addAttribute("campuses", campuses);
        model.addAttribute("selectedCampusId", campusId);
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("currentViewingCampus", currentViewingCampus);
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("availableYears", years);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("userDept", user.getDepartment());
        model.addAttribute("userCampus", user.getCampus());
        model.addAttribute("totalCount", totalSubmittedCount);
        model.addAttribute("pendingCount", pendingReviewCount);
        model.addAttribute("completedCount", completedReviewCount);
        model.addAttribute("councilReviewedCount", councilReviewedCount);
        model.addAttribute("draftCount", draftReviewCount);

        return "nq52/review-list";
    }

    /**
     * Bản in chuẩn công văn Mẫu 03
     */
    @GetMapping("/evaluations/{id}/print")
    public String printEvaluation(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal CurrentUser currentUser,
                                  Model model) {
        User user = getUser(currentUser);
        Nq52Evaluation eval = evaluationService.getEvaluationForView(id, user);
        Nq52EvaluationForm form = evaluationService.populateForm(eval);

        model.addAttribute("eval", eval);
        model.addAttribute("form", form);

        List<com.school.lessonplan.nq52.signature.dto.SignatureItemDto> signatures = signatureService.getSignaturesForDocument("EVALUATION", id);
        model.addAttribute("signatures", signatures);
        model.addAttribute("sigTeacher", getSigByRole(signatures, "TEACHER"));
        model.addAttribute("sigDeptLeader", getSigByRole(signatures, "DEPT_LEADER"));
        model.addAttribute("sigPrincipal", getSigByRole(signatures, "PRINCIPAL"));
        model.addAttribute("docType", "EVALUATION");
        model.addAttribute("docId", id);

        return "nq52/evaluation-print";
    }

    private com.school.lessonplan.nq52.signature.dto.SignatureItemDto getSigByRole(
            List<com.school.lessonplan.nq52.signature.dto.SignatureItemDto> list, String role) {
        if (list == null) return null;
        return list.stream()
                .filter(s -> role.equalsIgnoreCase(s.getSigningRole()) && s.isValid())
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private User getUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Vui lòng đăng nhập để tiếp tục.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin tài khoản."));
    }
}

