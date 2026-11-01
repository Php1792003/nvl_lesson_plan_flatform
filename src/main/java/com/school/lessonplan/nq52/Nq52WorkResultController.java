package com.school.lessonplan.nq52;

import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
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
@RequestMapping("/nq52/work-results")
public class Nq52WorkResultController {

    private final Nq52WorkResultService workResultService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;

    public Nq52WorkResultController(Nq52WorkResultService workResultService,
                                    UserRepository userRepository,
                                    DepartmentRepository departmentRepository,
                                    CampusRepository campusRepository) {
        this.workResultService = workResultService;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
    }

    /**
     * Danh sách bảng tự đánh giá kết quả:
     * - Giáo viên: Bảng tự đánh giá của cá nhân.
     * - Tổ trưởng & Ban Giám Hiệu: Hỗ trợ duyệt bảng kết quả của giáo viên trong tổ/trường,
     *   chọn Điểm trường / Phân hiệu, và chuyển đổi linh hoạt sang bảng cá nhân.
     */
    @GetMapping("")
    public String workResultsList(@AuthenticationPrincipal CurrentUser currentUser,
                                  @RequestParam(name = "tab", required = false) String tab,
                                  @RequestParam(name = "campusId", required = false) Long campusId,
                                  @RequestParam(name = "deptId", required = false) Long deptId,
                                  @RequestParam(name = "status", required = false) String statusStr,
                                  @RequestParam(name = "month", required = false) Integer month,
                                  @RequestParam(name = "year", required = false) Integer year,
                                  Model model) {
        User user = getUser(currentUser);
        boolean isReviewer = workResultService.isReviewer(user);
        boolean isAdmin = workResultService.isAdmin(user);

        String activeTab = tab;
        if (activeTab == null || activeTab.isBlank()) {
            activeTab = isReviewer ? "review" : "my";
        } else if (!isReviewer) {
            activeTab = "my";
        }

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = List.of(currentYear + 1, currentYear, currentYear - 1, currentYear - 2);

        var campuses = campusRepository.findAll();
        var departments = departmentRepository.findAll();

        // Nếu là Tổ trưởng (không phải Admin): Cố định campusId và deptId theo tài khoản của Tổ trưởng
        if (isReviewer && !isAdmin) {
            campusId = (user.getCampus() != null) ? user.getCampus().getId() : null;
            deptId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;
        }

        // Điểm trường / Phân hiệu đang xem
        com.school.lessonplan.campus.Campus currentViewingCampus = null;
        if (campusId != null) {
            final Long cid = campusId;
            currentViewingCampus = campuses.stream().filter(c -> c.getId().equals(cid)).findFirst().orElse(null);
        } else if (!isAdmin && user.getCampus() != null) {
            currentViewingCampus = user.getCampus();
        }

        // 1. Danh sách bảng tự đánh giá cá nhân
        List<Nq52WorkResult> myWorkResults = workResultService.getMyWorkResults(user.getId(), month, year);
        long myTotalCount = myWorkResults.size();
        long myDraftCount = myWorkResults.stream().filter(w -> w.getStatus() == Nq52Status.DRAFT).count();
        long mySubmittedCount = myWorkResults.stream().filter(w -> w.getStatus() == Nq52Status.SUBMITTED).count();
        long myReviewedCount = myWorkResults.stream().filter(w -> w.getStatus() == Nq52Status.REVIEWED).count();

        // 2. Danh sách bảng kết quả cần duyệt (cho Reviewer)
        List<Nq52WorkResult> reviewWorkResults = List.of();
        long pendingReviewCount = 0;
        long completedReviewCount = 0;
        long totalReviewCount = 0;
        String selectedStatus = (statusStr != null && !statusStr.isBlank()) ? statusStr.toUpperCase() : "SUBMITTED";

        if (isReviewer) {
            Nq52Status filterStatus = null;
            if ("SUBMITTED".equals(selectedStatus)) {
                filterStatus = Nq52Status.SUBMITTED;
            } else if ("REVIEWED".equals(selectedStatus)) {
                filterStatus = Nq52Status.REVIEWED;
            }

            List<Nq52WorkResult> allDept = workResultService.getDepartmentWorkResults(user, campusId, deptId, null, null, null);
            pendingReviewCount = allDept.stream().filter(r -> r.getStatus() == Nq52Status.SUBMITTED).count();
            completedReviewCount = allDept.stream().filter(r -> r.getStatus() == Nq52Status.REVIEWED).count();
            totalReviewCount = pendingReviewCount + completedReviewCount;

            reviewWorkResults = workResultService.getDepartmentWorkResults(user, campusId, deptId, filterStatus, month, year);
            reviewWorkResults = reviewWorkResults.stream().filter(r -> r.getStatus() != Nq52Status.DRAFT).toList();
        }

        model.addAttribute("pageTitle", "Tự đánh giá kết quả thực hiện nhiệm vụ (Mẫu 01TKNV)");
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("isReviewer", isReviewer);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("user", user);
        model.addAttribute("userCampus", user.getCampus());
        model.addAttribute("userDept", user.getDepartment());
        model.addAttribute("campuses", campuses);
        model.addAttribute("departments", departments);
        model.addAttribute("selectedCampusId", campusId);
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("currentViewingCampus", currentViewingCampus);
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("availableYears", years);

        // Thuộc tính tab Cá nhân
        model.addAttribute("workResults", myWorkResults);
        model.addAttribute("totalCount", myTotalCount);
        model.addAttribute("draftCount", myDraftCount);
        model.addAttribute("submittedCount", mySubmittedCount);
        model.addAttribute("reviewedCount", myReviewedCount);

        // Thuộc tính tab Duyệt cấp Tổ / Trường
        model.addAttribute("reviewWorkResults", reviewWorkResults);
        model.addAttribute("pendingCount", pendingReviewCount);
        model.addAttribute("completedCount", completedReviewCount);
        model.addAttribute("totalReviewCount", totalReviewCount);

        return "nq52/work-result-list";
    }

    /**
     * Tạo mới hoặc mở bảng tự đánh giá kết quả theo tháng/năm
     */
    @GetMapping("/create")
    public String createOrOpen(@RequestParam(name = "month", required = false) Integer month,
                               @RequestParam(name = "year", required = false) Integer year,
                               @RequestParam(name = "fresh", required = false, defaultValue = "false") Boolean fresh,
                               @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        int m = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();
        int y = (year != null && year >= 2020) ? year : LocalDate.now().getYear();

        Nq52WorkResult wr = workResultService.getOrCreateWorkResult(user, m, y, Boolean.TRUE.equals(fresh));
        return "redirect:/nq52/work-results/" + wr.getId();
    }

    /**
     * Xem / Soạn thảo bảng tự đánh giá kết quả
     */
    @GetMapping("/{id}")
    public String viewOrEdit(@PathVariable("id") Long id,
                             @AuthenticationPrincipal CurrentUser currentUser,
                             Model model) {
        User user = getUser(currentUser);
        Nq52WorkResult wr = workResultService.getWorkResultForView(id, user);

        boolean isOwner = wr.getUser().getId().equals(user.getId());
        boolean canEdit = isOwner && wr.getStatus() == Nq52Status.DRAFT;
        boolean canReview = workResultService.canReview(user, wr) && wr.getStatus() != Nq52Status.DRAFT;

        Nq52WorkResultForm form = workResultService.populateForm(wr);

        model.addAttribute("pageTitle", "Bảng tự đánh giá kết quả (Mẫu 01TKNV) - Tháng " + wr.getEvaluationMonth() + "/" + wr.getEvaluationYear());
        model.addAttribute("wr", wr);
        model.addAttribute("form", form);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("canReview", canReview);
        model.addAttribute("isReviewer", workResultService.isReviewer(user));

        return "nq52/work-result-form";
    }

    /**
     * Lưu bản nháp hoặc nộp bảng tự đánh giá kết quả
     */
    @PostMapping("/{id}/save")
    public String saveWorkResult(@PathVariable("id") Long id,
                                 @ModelAttribute("form") Nq52WorkResultForm form,
                                 @AuthenticationPrincipal CurrentUser currentUser,
                                 RedirectAttributes redirectAttributes) {
        User user = getUser(currentUser);
        boolean isSubmit = "SUBMIT".equalsIgnoreCase(form.getAction());
        boolean isExport = "EXPORT".equalsIgnoreCase(form.getAction()) || "SAVE_AND_EXPORT".equalsIgnoreCase(form.getAction());

        try {
            Nq52WorkResult saved = workResultService.saveWorkResult(id, form, user, isSubmit);
            if (isExport) {
                return "redirect:/nq52/work-results/" + id + "/export";
            }
            if (isSubmit) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã nộp Bảng tự đánh giá kết quả Tháng " + saved.getEvaluationMonth() + "/" + saved.getEvaluationYear() + " thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã lưu bản nháp Bảng tự đánh giá kết quả thành công!");
            }
        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi lưu: " + e.getMessage());
        }

        return "redirect:/nq52/work-results/" + id;
    }

    /**
     * Tổ trưởng chuyên môn / Ban Giám Hiệu xét duyệt & ký xác nhận bảng kết quả
     */
    @PostMapping("/{id}/review")
    public String reviewWorkResult(@PathVariable("id") Long id,
                                   @RequestParam(name = "action", required = false, defaultValue = "APPROVE") String action,
                                   @RequestParam(name = "reviewerNotes", required = false) String reviewerNotes,
                                   @AuthenticationPrincipal CurrentUser currentUser,
                                   RedirectAttributes redirectAttributes) {
        User user = getUser(currentUser);
        try {
            if ("REJECT".equalsIgnoreCase(action)) {
                workResultService.rejectWorkResult(id, reviewerNotes, user);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã trả lại bảng tự đánh giá để giáo viên bổ sung, chỉnh sửa.");
            } else {
                Nq52WorkResult reviewed = workResultService.reviewWorkResult(id, reviewerNotes, user);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã ký duyệt và xác nhận Bảng tự đánh giá kết quả Tháng "
                                + reviewed.getEvaluationMonth() + "/" + reviewed.getEvaluationYear() + " thành công!");
            }
        } catch (IllegalStateException | AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/nq52/work-results/" + id;
    }

    /**
     * Xuất file Excel Mẫu 01TKNV (.xlsx) và lưu trữ vào database & disk
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportAndDownload(@PathVariable("id") Long id,
                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        Nq52WorkResult wr = workResultService.exportAndSaveExcel(id, user);
        byte[] data = wr.getExportedFileData();

        String filename = wr.getExportedFileName() != null ? wr.getExportedFileName() : "Bang_Tu_Danh_Gia_Ket_Qua_01TKNV.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(data);
    }

    /**
     * Tải file Excel đã lưu trữ trước đó trong CSDL
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadSavedFile(@PathVariable("id") Long id,
                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        Nq52WorkResult wr = workResultService.getWorkResultForView(id, user);
        byte[] data = workResultService.getExportedFileData(id, user);

        String filename = wr.getExportedFileName() != null ? wr.getExportedFileName() : "Bang_Tu_Danh_Gia_Ket_Qua_01TKNV.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(data);
    }

    /**
     * Bản in chuẩn công văn Mẫu 01TKNV (khổ ngang landscape)
     */
    @GetMapping("/{id}/print")
    public String printWorkResult(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal CurrentUser currentUser,
                                  Model model) {
        User user = getUser(currentUser);
        Nq52WorkResult wr = workResultService.getWorkResultForView(id, user);
        Nq52WorkResultForm form = workResultService.populateForm(wr);

        model.addAttribute("pageTitle", "In Mẫu 01TKNV - Bảng thống kê, tự đánh giá kết quả");
        model.addAttribute("wr", wr);
        model.addAttribute("form", form);

        return "nq52/work-result-print";
    }

    private User getUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy người dùng."));
    }
}

