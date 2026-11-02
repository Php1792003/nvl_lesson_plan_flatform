package com.school.lessonplan.nq52;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
import com.school.lessonplan.nq52.dto.Nq52WorkResultTaskItem;
import com.school.lessonplan.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class Nq52WorkResultService {

    private static final Logger log = LoggerFactory.getLogger(Nq52WorkResultService.class);

    public static final String DIR_EXCEL = "nq52/nq52_tudanhgiaketqua";

    private final Nq52WorkResultRepository workResultRepository;
    private final ObjectMapper objectMapper;
    private final Nq52ExcelExportService excelExportService;
    private final AppProperties appProperties;
    private final com.school.lessonplan.nq52.signature.Nq52DigitalSignatureRepository signatureRepository;

    public Nq52WorkResultService(Nq52WorkResultRepository workResultRepository,
                                 ObjectMapper objectMapper,
                                 Nq52ExcelExportService excelExportService,
                                 AppProperties appProperties,
                                 com.school.lessonplan.nq52.signature.Nq52DigitalSignatureRepository signatureRepository) {
        this.workResultRepository = workResultRepository;
        this.objectMapper = objectMapper;
        this.excelExportService = excelExportService;
        this.appProperties = appProperties;
        this.signatureRepository = signatureRepository;
        initStorageDirectory();
    }

    private void initStorageDirectory() {
        if (appProperties != null && appProperties.getStorage() != null && appProperties.getStorage().getRoot() != null) {
            try {
                Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
                Files.createDirectories(rootPath.resolve(DIR_EXCEL));
            } catch (Exception e) {
                log.warn("Không thể tạo thư mục lưu trữ Excel kết quả NQ52: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public List<Nq52WorkResult> getMyWorkResults(Long userId, Integer month, Integer year) {
        List<Nq52WorkResult> list = workResultRepository.findMyWorkResultsFiltered(userId, month, year);
        for (Nq52WorkResult wr : list) {
            ensureDefaultTasksIfEmpty(wr);
        }
        return list;
    }

    @Transactional
    public List<Nq52WorkResult> getDepartmentWorkResults(User reviewer, Long filterCampusId, Long filterDepartmentId,
                                                         Nq52Status filterStatus, Integer filterMonth, Integer filterYear) {
        boolean isAdmin = isAdmin(reviewer);
        Long deptId = filterDepartmentId;
        Long campusId = filterCampusId;

        if (!isAdmin) {
            // Tổ trưởng chỉ xem và duyệt được giáo viên cùng tổ môn và cùng điểm trường/phân hiệu
            if (reviewer.getDepartment() == null || reviewer.getCampus() == null) {
                return Collections.emptyList();
            }
            deptId = reviewer.getDepartment().getId();
            campusId = reviewer.getCampus().getId();
        }

        List<Nq52WorkResult> list = workResultRepository.findDepartmentWorkResultsFiltered(campusId, deptId, filterStatus, filterMonth, filterYear);
        for (Nq52WorkResult wr : list) {
            ensureDefaultTasksIfEmpty(wr);
        }
        return list;
    }

    public void ensureDefaultTasksIfEmpty(Nq52WorkResult wr) {
        if (wr == null) return;
        List<Nq52WorkResultTaskItem> tasks = parseTasksJson(wr.getTasksDataJson());
        if (tasks.isEmpty()) {
            tasks = createBlankTasks();
            try {
                wr.setTasksDataJson(objectMapper.writeValueAsString(tasks));
            } catch (Exception ignored) {}
            recalculateSummary(wr, tasks);
            workResultRepository.save(wr);
        } else if (wr.getTotalAssigned() == 0 || wr.getTotalAssigned() != tasks.size()) {
            recalculateSummary(wr, tasks);
            workResultRepository.save(wr);
        }
    }

    @Transactional(readOnly = true)
    public Nq52WorkResult getWorkResultForView(Long id, User currentUser) {
        Nq52WorkResult wr = workResultRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bảng tự đánh giá kết quả ID: " + id));

        if (!canView(currentUser, wr)) {
            log.warn("User {} cố gắng truy cập trái phép bảng kết quả ID {}", currentUser.getEmail(), id);
            throw new AccessDeniedException("Bạn không có quyền truy cập bảng kết quả này.");
        }

        return wr;
    }

    public Nq52WorkResult getOrCreateWorkResult(User user, int month, int year) {
        return getOrCreateWorkResult(user, month, year, false);
    }

    public Nq52WorkResult getOrCreateWorkResult(User user, int month, int year, boolean fresh) {
        var existing = workResultRepository.findByUserIdAndEvaluationMonthAndEvaluationYear(user.getId(), month, year);
        if (existing.isPresent()) {
            Nq52WorkResult wr = existing.get();
            if (fresh) {
                // Người dùng yêu cầu làm mới mẫu trắng để tự nhập công việc từ đầu
                wr.setStatus(Nq52Status.DRAFT);
                wr.setSubmittedAt(null);
                wr.setReviewedAt(null);
                wr.setReviewer(null);
                wr.setReviewerNotes(null);
                wr.setExportedFileName(null);
                wr.setExportedFileData(null);
                wr.setExportedFilePath(null);
                wr.setSummaryNotes("");

                List<Nq52WorkResultTaskItem> blankTasks = createBlankTasks();
                try {
                    wr.setTasksDataJson(objectMapper.writeValueAsString(blankTasks));
                } catch (Exception e) {
                    wr.setTasksDataJson("[]");
                }
                recalculateSummary(wr, blankTasks);
                log.info("Đã làm mới mẫu tự đánh giá kết quả Tháng {}/{} cho giáo viên {}", month, year, user.getEmail());
                return workResultRepository.save(wr);
            }
            return wr;
        }

        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setUser(user);
        wr.setDepartment(user.getDepartment());
        wr.setEvaluationMonth(month);
        wr.setEvaluationYear(year);
        wr.setStatus(Nq52Status.DRAFT);
        wr.setTeacherNameSnapshot(user.getFullName());
        wr.setPositionTitle("Giáo viên");
        wr.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        wr.setDepartmentNameSnapshot(user.getDepartment() != null ? user.getDepartment().getName() : "Toán học");
        wr.setCampusNameSnapshot(user.getCampus() != null ? user.getCampus().getName() : "Điểm trường chính");
        wr.setEvaluationDateStr("Cẩm Lệ, ngày 30 tháng " + month + " năm " + year);

        // Tạo sẵn mẫu trắng với 4 nhóm công việc để giáo viên tự nhập
        List<Nq52WorkResultTaskItem> blankTasks = createBlankTasks();
        try {
            wr.setTasksDataJson(objectMapper.writeValueAsString(blankTasks));
        } catch (Exception e) {
            wr.setTasksDataJson("[]");
        }
        recalculateSummary(wr, blankTasks);

        return workResultRepository.save(wr);
    }

    public Nq52WorkResult saveWorkResult(Long id, Nq52WorkResultForm form, User currentUser, boolean isSubmit) {
        Nq52WorkResult wr = workResultRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bảng kết quả ID: " + id));

        if (!wr.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn chỉ được chỉnh sửa bảng tự đánh giá kết quả của chính mình.");
        }

        if (wr.getStatus() != Nq52Status.DRAFT) {
            throw new IllegalStateException("Bảng tự đánh giá đã được nộp hoặc đã được đánh giá, không thể chỉnh sửa.");
        }

        if (form.getPositionTitle() != null && !form.getPositionTitle().isBlank()) {
            wr.setPositionTitle(form.getPositionTitle().trim());
        }
        if (form.getEvaluationDateStr() != null && !form.getEvaluationDateStr().isBlank()) {
            wr.setEvaluationDateStr(form.getEvaluationDateStr().trim());
        }
        wr.setSummaryNotes(form.getSummaryNotes());

        List<Nq52WorkResultTaskItem> tasks = form.getTasks() != null ? form.getTasks() : new ArrayList<>();
        // Filter out empty items
        tasks = tasks.stream()
                .filter(t -> t.getContent() != null && !t.getContent().isBlank())
                .toList();

        try {
            wr.setTasksDataJson(objectMapper.writeValueAsString(tasks));
        } catch (Exception e) {
            log.error("Lỗi parse JSON tasks", e);
        }

        recalculateSummary(wr, tasks);

        if (isSubmit) {
            wr.setStatus(Nq52Status.SUBMITTED);
            wr.setSubmittedAt(Instant.now());
            log.info("Giáo viên {} đã nộp bảng tự đánh giá kết quả Tháng {}/{}",
                    currentUser.getEmail(), wr.getEvaluationMonth(), wr.getEvaluationYear());
        }

        return workResultRepository.save(wr);
    }

    public Nq52WorkResult reviewWorkResult(Long id, String reviewerNotes, User reviewer) {
        Nq52WorkResult wr = workResultRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bảng kết quả ID: " + id));

        if (!canReview(reviewer, wr)) {
            throw new AccessDeniedException("Bạn không có quyền đánh giá bảng kết quả của giáo viên này.");
        }

        if (wr.getStatus() == Nq52Status.DRAFT) {
            throw new IllegalStateException("Giáo viên chưa nộp bảng tự đánh giá kết quả, Tổ trưởng chưa thể ký duyệt.");
        }

        wr.setReviewer(reviewer);
        wr.setReviewerNameSnapshot(reviewer.getFullName());
        wr.setReviewerNotes(reviewerNotes);
        wr.setStatus(Nq52Status.REVIEWED);
        wr.setReviewedAt(Instant.now());

        Nq52WorkResult saved = workResultRepository.save(wr);

        // Tự động xuất và lưu file Excel mới nhất kèm chữ ký người duyệt vào thư mục lưu trữ
        try {
            exportAndSaveExcel(saved.getId(), reviewer);
        } catch (Exception e) {
            log.warn("Không thể tự động xuất lưu file Excel sau khi ký duyệt: {}", e.getMessage());
        }
        return saved;
    }

    public Nq52WorkResult rejectWorkResult(Long id, String reason, User reviewer) {
        Nq52WorkResult wr = workResultRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bảng kết quả ID: " + id));

        if (!canReview(reviewer, wr)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên bảng kết quả này.");
        }

        wr.setStatus(Nq52Status.DRAFT);
        wr.setReviewerNotes(reason != null && !reason.isBlank() ? "Yêu cầu chỉnh sửa: " + reason : wr.getReviewerNotes());
        wr.setSubmittedAt(null);
        return workResultRepository.save(wr);
    }

    public Nq52WorkResult exportAndSaveExcel(Long id, User currentUser) {
        Nq52WorkResult wr = getWorkResultForView(id, currentUser);
        ensureDefaultTasksIfEmpty(wr);
        Nq52WorkResultForm form = populateForm(wr);

        List<com.school.lessonplan.nq52.signature.Nq52DigitalSignature> signatures =
                signatureRepository.findByDocumentTypeAndDocumentIdOrderBySignedAtAsc("WORK_RESULT", id);
        byte[] excelBytes = excelExportService.generateWorkResultExcel(wr, form, signatures);

        String cleanName = removeAccents(wr.getTeacherNameSnapshot()).replaceAll("[^a-zA-Z0-9_]", "_");
        String filename = "Bang_Tu_Danh_Gia_Ket_Qua_Thang_" + wr.getEvaluationMonth() + "_" + wr.getEvaluationYear() + "_" + cleanName + ".xlsx";

        wr.setExportedFileName(filename);
        wr.setExportedFileSize((long) excelBytes.length);
        wr.setExportedFileType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        wr.setExportedAt(Instant.now());
        wr.setExportedFileData(excelBytes);

        // Lưu file vật lý vào uploads/nq52/nq52_tudanhgiaketqua/
        try {
            if (appProperties != null && appProperties.getStorage() != null && appProperties.getStorage().getRoot() != null) {
                Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
                Path targetDir = rootPath.resolve(DIR_EXCEL);
                Files.createDirectories(targetDir);

                String storedFilename = UUID.randomUUID().toString() + ".xlsx";
                Path targetPath = targetDir.resolve(storedFilename);
                Files.write(targetPath, excelBytes);

                wr.setExportedFilePath(DIR_EXCEL + "/" + storedFilename);
                log.info("Đã lưu file Excel kết quả NQ52 vào: {}/{}", DIR_EXCEL, storedFilename);
            }
        } catch (Exception e) {
            log.warn("Không thể lưu file Excel lên đĩa (đã lưu an toàn trong database): {}", e.getMessage());
        }

        return workResultRepository.save(wr);
    }

    public byte[] getExportedFileData(Long id, User currentUser) {
        Nq52WorkResult wr = getWorkResultForView(id, currentUser);
        // Luôn làm mới file kèm chữ ký số mới nhất
        wr = exportAndSaveExcel(id, currentUser);
        return wr.getExportedFileData();
    }

    public Nq52WorkResultForm populateForm(Nq52WorkResult wr) {
        Nq52WorkResultForm form = new Nq52WorkResultForm();
        form.setId(wr.getId());
        form.setMonth(wr.getEvaluationMonth());
        form.setYear(wr.getEvaluationYear());
        form.setPositionTitle(wr.getPositionTitle());
        form.setWorkUnit(wr.getWorkUnit());
        form.setEvaluationDateStr(wr.getEvaluationDateStr());
        form.setSummaryNotes(wr.getSummaryNotes());
        form.setReviewerNotes(wr.getReviewerNotes());

        List<Nq52WorkResultTaskItem> tasks = parseTasksJson(wr.getTasksDataJson());
        form.setTasks(tasks);

        // Luôn tính toán lại số liệu thống kê chuẩn xác từ danh sách nhiệm vụ
        recalculateSummary(wr, tasks);

        form.setTotalAssigned(wr.getTotalAssigned());
        form.setTotalCompleted(wr.getTotalCompleted());
        form.setTotalUncompleted(wr.getTotalUncompleted());
        form.setCompletionRate(wr.getCompletionRate() != null ? wr.getCompletionRate().doubleValue() : 0.0);

        form.setProgressEarly(wr.getProgressEarly());
        form.setProgressOnTime(wr.getProgressOnTime());
        form.setProgressLate(wr.getProgressLate());
        form.setOnTimeRate(wr.getOnTimeRate() != null ? wr.getOnTimeRate().doubleValue() : 0.0);

        form.setQualityStandard(wr.getQualityStandard());
        form.setQualityExceeded(wr.getQualityExceeded());
        form.setQualitySubstandard(wr.getQualitySubstandard());
        form.setQualityRate(wr.getQualityRate() != null ? wr.getQualityRate().doubleValue() : 0.0);

        return form;
    }

    public void recalculateSummary(Nq52WorkResult wr, List<Nq52WorkResultTaskItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            wr.setTotalAssigned(0);
            wr.setTotalCompleted(0);
            wr.setTotalUncompleted(0);
            wr.setCompletionRate(BigDecimal.ZERO);
            wr.setProgressEarly(0);
            wr.setProgressOnTime(0);
            wr.setProgressLate(0);
            wr.setOnTimeRate(BigDecimal.ZERO);
            wr.setQualityStandard(0);
            wr.setQualityExceeded(0);
            wr.setQualitySubstandard(0);
            wr.setQualityRate(BigDecimal.ZERO);
            return;
        }

        // Chỉ tính các công việc có nội dung (không tính dòng trắng đang chờ người dùng nhập)
        List<Nq52WorkResultTaskItem> validTasks = tasks.stream()
                .filter(t -> t.getContent() != null && !t.getContent().isBlank())
                .toList();

        if (validTasks.isEmpty()) {
            wr.setTotalAssigned(0);
            wr.setTotalCompleted(0);
            wr.setTotalUncompleted(0);
            wr.setCompletionRate(BigDecimal.ZERO);
            wr.setProgressEarly(0);
            wr.setProgressOnTime(0);
            wr.setProgressLate(0);
            wr.setOnTimeRate(BigDecimal.ZERO);
            wr.setQualityStandard(0);
            wr.setQualityExceeded(0);
            wr.setQualitySubstandard(0);
            wr.setQualityRate(BigDecimal.ZERO);
            return;
        }

        int total = validTasks.size();
        int completed = 0;
        int uncompleted = 0;
        int early = 0;
        int onTime = 0;
        int late = 0;
        int standard = 0;
        int exceeded = 0;
        int substandard = 0;

        for (Nq52WorkResultTaskItem t : validTasks) {
            if ("COMPLETED".equalsIgnoreCase(t.getStatus())) {
                completed++;
            } else {
                uncompleted++;
            }

            if ("EARLY".equalsIgnoreCase(t.getTimeProgress())) early++;
            else if ("ON_TIME".equalsIgnoreCase(t.getTimeProgress())) onTime++;
            else if ("LATE".equalsIgnoreCase(t.getTimeProgress())) late++;

            if ("EXCEEDED".equalsIgnoreCase(t.getQuality())) exceeded++;
            else if ("STANDARD".equalsIgnoreCase(t.getQuality())) standard++;
            else if ("SUBSTANDARD".equalsIgnoreCase(t.getQuality())) substandard++;
        }

        wr.setTotalAssigned(total);
        wr.setTotalCompleted(completed);
        wr.setTotalUncompleted(uncompleted);

        double compRate = total > 0 ? (double) completed / total * 100.0 : 0.0;
        wr.setCompletionRate(BigDecimal.valueOf(compRate).setScale(2, RoundingMode.HALF_UP));

        wr.setProgressEarly(early);
        wr.setProgressOnTime(onTime);
        wr.setProgressLate(late);

        double onTimeR = total > 0 ? (double) (early + onTime) / total * 100.0 : 0.0;
        wr.setOnTimeRate(BigDecimal.valueOf(onTimeR).setScale(2, RoundingMode.HALF_UP));

        wr.setQualityStandard(standard);
        wr.setQualityExceeded(exceeded);
        wr.setQualitySubstandard(substandard);

        double qualR = total > 0 ? (double) (standard + exceeded) / total * 100.0 : 0.0;
        wr.setQualityRate(BigDecimal.valueOf(qualR).setScale(2, RoundingMode.HALF_UP));
    }

    public List<Nq52WorkResultTaskItem> parseTasksJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Nq52WorkResultTaskItem>>() {});
        } catch (Exception e) {
            log.error("Lỗi khi đọc JSON nhiệm vụ: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean canView(User user, Nq52WorkResult wr) {
        if (user == null || wr == null) return false;
        if (wr.getUser().getId().equals(user.getId())) return true;
        if (isAdmin(user)) return true;
        return isDeptLeaderOf(user, wr);
    }

    public boolean canReview(User user, Nq52WorkResult wr) {
        if (user == null || wr == null) return false;
        if (isAdmin(user)) return true;
        return isDeptLeaderOf(user, wr);
    }

    private static final String PERM_NQ52_REVIEW = "NQ52_REVIEW";

    public boolean isDeptLeaderOf(User user, Nq52WorkResult wr) {
        if (user == null || wr == null || user.getDepartment() == null || user.getCampus() == null) return false;
        boolean isLeader = hasRole(user, "DEPT_LEADER") || hasPermission(user, PERM_NQ52_REVIEW);
        if (!isLeader) return false;

        boolean matchDept = wr.getDepartment() != null && user.getDepartment().getId().equals(wr.getDepartment().getId());
        boolean matchCampus = wr.getUser() != null && wr.getUser().getCampus() != null
                && user.getCampus().getId().equals(wr.getUser().getCampus().getId());

        return matchDept && matchCampus;
    }

    public boolean isAdmin(User user) {
        return hasRole(user, "ADMIN");
    }

    public boolean isReviewer(User user) {
        if (user == null) return false;
        if (isAdmin(user)) return true;
        return hasRole(user, "DEPT_LEADER") || hasPermission(user, PERM_NQ52_REVIEW);
    }

    private boolean hasRole(User user, String roleCode) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> roleCode.equalsIgnoreCase(r.getCode()));
    }

    private boolean hasPermission(User user, String permCode) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream()
                .filter(r -> r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> permCode.equalsIgnoreCase(p.getCode()));
    }

    public List<Nq52WorkResultTaskItem> createBlankTasks() {
        List<Nq52WorkResultTaskItem> list = new ArrayList<>();
        list.add(createTask("task_1", "I", 1, "", "NORMAL", "LEAD", "COMPLETED", "", "ON_TIME", "STANDARD"));
        list.add(createTask("task_2", "II", 1, "", "NORMAL", "LEAD", "COMPLETED", "", "ON_TIME", "STANDARD"));
        list.add(createTask("task_3", "III", 1, "", "NORMAL", "LEAD", "COMPLETED", "", "ON_TIME", "STANDARD"));
        list.add(createTask("task_4", "IV", 1, "", "NORMAL", "LEAD", "COMPLETED", "", "ON_TIME", "STANDARD"));
        return list;
    }

    private List<Nq52WorkResultTaskItem> createDefaultTasks(int month, int year) {
        List<Nq52WorkResultTaskItem> list = new ArrayList<>();
        // Nhóm I
        list.add(createTask("task_1", "I", 1, "Thực hiện giảng dạy đúng chương trình, phân phối chương trình Tháng " + month + "/" + year, "NORMAL", "LEAD", "COMPLETED", "01/" + month + " - 30/" + month, "ON_TIME", "STANDARD"));
        list.add(createTask("task_2", "I", 2, "Xây dựng kế hoạch bài dạy (giáo án) đầy đủ, đúng quy định chuyên môn", "NORMAL", "LEAD", "COMPLETED", "Hàng tuần", "ON_TIME", "STANDARD"));
        // Nhóm II
        list.add(createTask("task_3", "II", 1, "Thao giảng, dự giờ đồng nghiệp theo kế hoạch của tổ chuyên môn", "NORMAL", "COORDINATE", "COMPLETED", "Trong tháng", "ON_TIME", "STANDARD"));
        list.add(createTask("task_4", "II", 2, "Bồi dưỡng học sinh giỏi và phụ đạo học sinh chưa đạt chuẩn kiến thức", "COMPLEX", "LEAD", "COMPLETED", "Thường xuyên", "EARLY", "EXCEEDED"));
        // Nhóm III
        list.add(createTask("task_5", "III", 1, "Hỗ trợ công tác kiểm tra định kỳ, coi thi và chấm bài theo phân công của nhà trường", "NORMAL", "COORDINATE", "COMPLETED", "Theo lịch", "ON_TIME", "STANDARD"));
        // Nhóm IV
        list.add(createTask("task_6", "IV", 1, "Tham gia sinh hoạt chuyên môn, hội họp hội đồng sư phạm và tập huấn bồi dưỡng", "NORMAL", "COORDINATE", "COMPLETED", "Hàng tuần", "ON_TIME", "STANDARD"));
        return list;
    }

    private Nq52WorkResultTaskItem createTask(String id, String cat, int order, String content, String nature, String role, String status, String timeReq, String timeProg, String quality) {
        Nq52WorkResultTaskItem item = new Nq52WorkResultTaskItem(id, cat, order, content);
        item.setNature(nature);
        item.setRole(role);
        item.setStatus(status);
        item.setTimeRequired(timeReq);
        item.setTimeProgress(timeProg);
        item.setQuality(quality);
        return item;
    }

    private String removeAccents(String text) {
        if (text == null) return "Giao_Vien";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
}

