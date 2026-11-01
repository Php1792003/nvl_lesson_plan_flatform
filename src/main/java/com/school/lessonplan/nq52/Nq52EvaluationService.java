package com.school.lessonplan.nq52;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.nq52.dto.Nq52EvaluationForm;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
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
public class Nq52EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(Nq52EvaluationService.class);

    public static final String NQ52_DIR = "nq52";
    public static final String DIR_CAN_NHAN = "nq52/nq52_canhandanhgia";
    public static final String DIR_TO_TRUONG = "nq52/nq52_totruongdanhgia";
    public static final String DIR_BAN_GIAM_HIEU = "nq52/nq52_bangiamhieu";
    public static final String DIR_TU_DANH_GIA_KET_QUA = "nq52/nq52_tudanhgiaketqua";

    private final Nq52EvaluationRepository evaluationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Nq52DocxExportService docxExportService;
    private final AppProperties appProperties;

    public Nq52EvaluationService(Nq52EvaluationRepository evaluationRepository,
                                 DepartmentRepository departmentRepository,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper,
                                 Nq52DocxExportService docxExportService,
                                 AppProperties appProperties) {
        this.evaluationRepository = evaluationRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.docxExportService = docxExportService;
        this.appProperties = appProperties;
        initStorageDirectories();
    }

    /**
     * Khởi tạo cấu trúc các thư mục lưu trữ NQ52 nếu chưa tồn tại
     */
    private void initStorageDirectories() {
        if (appProperties != null && appProperties.getStorage() != null && appProperties.getStorage().getRoot() != null) {
            try {
                Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
                Files.createDirectories(rootPath.resolve(DIR_CAN_NHAN));
                Files.createDirectories(rootPath.resolve(DIR_TO_TRUONG));
                Files.createDirectories(rootPath.resolve(DIR_BAN_GIAM_HIEU));
                Files.createDirectories(rootPath.resolve(DIR_TU_DANH_GIA_KET_QUA));
            } catch (Exception e) {
                log.warn("Không thể tự động tạo cấu trúc thư mục lưu trữ NQ52: {}", e.getMessage());
            }
        }
    }

    /**
     * Lấy danh sách phiếu của cá nhân giáo viên
     */
    @Transactional(readOnly = true)
    public List<Nq52Evaluation> getMyEvaluations(Long userId) {
        return getMyEvaluations(userId, null, null);
    }

    @Transactional(readOnly = true)
    public List<Nq52Evaluation> getMyEvaluations(Long userId, Integer month, Integer year) {
        return evaluationRepository.findMyEvaluationsFiltered(userId, month, year);
    }

    /**
     * Lấy hoặc tạo mới phiếu đánh giá cho tháng/năm chỉ định
     */
    public Nq52Evaluation getOrCreateEvaluation(User user, int month, int year) {
        if (month < 1 || month > 12) month = LocalDate.now().getMonthValue();
        if (year < 2020 || year > 2050) year = LocalDate.now().getYear();

        final int fMonth = month;
        final int fYear = year;

        return evaluationRepository.findByUserIdAndEvaluationMonthAndEvaluationYear(user.getId(), fMonth, fYear)
                .orElseGet(() -> {
                    Nq52Evaluation evaluation = new Nq52Evaluation();
                    evaluation.setUser(user);
                    Department dept = user.getDepartment();
                    if (dept == null) {
                        dept = departmentRepository.findAll().stream().findFirst().orElse(null);
                    }
                    evaluation.setDepartment(dept);
                    evaluation.setEvaluationMonth(fMonth);
                    evaluation.setEvaluationYear(fYear);
                    evaluation.setStatus(Nq52Status.DRAFT);
                    evaluation.setTeacherNameSnapshot(user.getFullName());
                    evaluation.setPositionTitle("Giáo viên");
                    evaluation.setWorkUnit("Trường THCS Nguyễn Văn Linh");
                    evaluation.setDepartmentNameSnapshot(dept != null ? dept.getName() : "Chưa phân tổ");
                    
                    // Khởi tạo các giá trị mặc định cho form (để trống để giáo viên tự điền)
                    Map<String, Object> initialData = new HashMap<>();
                    initialData.put("selfScores", new HashMap<String, Double>());
                    initialData.put("explanations", new HashMap<String, String>());
                    try {
                        evaluation.setCriteriaDataJson(objectMapper.writeValueAsString(initialData));
                    } catch (Exception e) {
                        evaluation.setCriteriaDataJson("{}");
                    }
                    
                    return evaluationRepository.save(evaluation);
                });
    }

    /**
     * Lấy chi tiết phiếu và kiểm tra quyền truy cập:
     * - Giáo viên: Chỉ xem được phiếu của chính mình.
     * - Tổ trưởng: Xem được phiếu của giáo viên trong tổ mình phụ trách.
     * - Ban Giám Hiệu (ADMIN): Xem được toàn bộ phiếu của mọi tổ.
     */
    @Transactional(readOnly = true)
    public Nq52Evaluation getEvaluationForView(Long id, User currentUser) {
        Nq52Evaluation eval = evaluationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đánh giá ID: " + id));

        if (!canView(currentUser, eval)) {
            log.warn("User {} cố gắng truy cập trái phép phiếu ID {}", currentUser.getEmail(), id);
            throw new AccessDeniedException("Bạn không có quyền truy cập phiếu đánh giá này.");
        }

        return eval;
    }

    /**
     * Giáo viên lưu bản nháp hoặc nộp phiếu
     */
    public Nq52Evaluation saveTeacherEvaluation(Long id, Nq52EvaluationForm form, User currentUser, boolean isSubmit) {
        Nq52Evaluation eval = evaluationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đánh giá ID: " + id));

        // Kiểm tra quyền sở hữu
        if (!eval.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn chỉ được chỉnh sửa phiếu đánh giá của chính mình.");
        }

        // Nếu đã nộp hoặc đã được đánh giá thì khóa chỉnh sửa
        if (eval.getStatus() != Nq52Status.DRAFT) {
            throw new IllegalStateException("Phiếu đã được nộp hoặc đã được đánh giá, không thể chỉnh sửa.");
        }

        if (form.getPositionTitle() != null && !form.getPositionTitle().isBlank()) {
            eval.setPositionTitle(form.getPositionTitle().trim());
        }
        eval.setStrengths(form.getStrengths());
        eval.setWeaknesses(form.getWeaknesses());

        // 1. Tính điểm Phần A (Tối đa 30) - Ràng buộc cứng không vượt quá thang điểm tối đa
        double scoreA = 0.0;
        Map<String, Double> selfScores = sanitizeAndClampScores(form.getSelfScores());
        for (Map.Entry<String, Double> entry : Nq52CriteriaCatalog.MAX_SCORES_PART_A.entrySet()) {
            Double val = selfScores.get(entry.getKey());
            if (val != null && val > 0) {
                scoreA += val;
            }
        }
        scoreA = Math.min(scoreA, 30.0);
        eval.setSelfScoreA(BigDecimal.valueOf(scoreA).setScale(2, RoundingMode.HALF_UP));

        // 2. Tính điểm Phần B (Tối đa 70)
        double scoreB = Nq52CriteriaCatalog.getScoreB1(form.getSelectedB1())
                      + Nq52CriteriaCatalog.getScoreB2(form.getSelectedB2())
                      + Nq52CriteriaCatalog.getScoreB3(form.getSelectedB3());
        scoreB = Math.min(scoreB, 70.0);
        eval.setSelfScoreB(BigDecimal.valueOf(scoreB).setScale(2, RoundingMode.HALF_UP));

        // 3. Tính điểm Phần C (Điểm cộng & trừ)
        double scoreCPlus = 0.0;
        for (Map.Entry<String, Double> entry : Nq52CriteriaCatalog.MAX_SCORES_PART_C1.entrySet()) {
            Double val = selfScores.get(entry.getKey());
            if (val != null && val > 0) {
                scoreCPlus += Math.min(val, entry.getValue());
            }
        }
        scoreCPlus = Math.min(scoreCPlus, 6.0); // Tối đa 6 điểm cộng

        double scoreCMinus = 0.0;
        Double cMinus1 = selfScores.get("C_II_1");
        if (cMinus1 != null && cMinus1 > 0) scoreCMinus += cMinus1;
        Double cMinus2 = selfScores.get("C_II_2");
        if (cMinus2 != null && cMinus2 > 0) scoreCMinus += cMinus2;
        Double cMinus3 = selfScores.get("C_II_3");
        if (cMinus3 != null && cMinus3 > 0) scoreCMinus += cMinus3;

        eval.setSelfScoreCPlus(BigDecimal.valueOf(scoreCPlus).setScale(2, RoundingMode.HALF_UP));
        eval.setSelfScoreCMinus(BigDecimal.valueOf(scoreCMinus).setScale(2, RoundingMode.HALF_UP));

        // Tổng điểm tự chấm
        double total = scoreA + scoreB + scoreCPlus - scoreCMinus;
        if (total < 0) total = 0.0;
        eval.setSelfTotalScore(BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP));

        // Xếp loại tự đề xuất
        if (form.getSelfClassification() != null && !form.getSelfClassification().isBlank()) {
            eval.setSelfClassification(form.getSelfClassification().trim());
        } else {
            eval.setSelfClassification(Nq52CriteriaCatalog.determineClassification(eval.getSelfTotalScore()));
        }

        // Lưu toàn bộ chi tiết vào JSON
        Map<String, Object> data = parseJson(eval.getCriteriaDataJson());
        data.put("selectedB1", form.getSelectedB1());
        data.put("selectedB2", form.getSelectedB2());
        data.put("selectedB3", form.getSelectedB3());
        data.put("selfScores", selfScores);
        data.put("explanations", form.getExplanations() != null ? form.getExplanations() : new HashMap<>());
        try {
            eval.setCriteriaDataJson(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.error("Lỗi khi lưu JSON tiêu chí NQ52", e);
        }

        // Cập nhật trạng thái nếu nộp
        if (isSubmit) {
            eval.setStatus(Nq52Status.SUBMITTED);
            eval.setSubmittedAt(Instant.now());
            log.info("Giáo viên {} đã nộp phiếu NQ52 ID {}", currentUser.getEmail(), eval.getId());
        } else {
            log.info("Giáo viên {} đã lưu nháp phiếu NQ52 ID {}", currentUser.getEmail(), eval.getId());
        }

        return evaluationRepository.save(eval);
    }

    /**
     * Tổ trưởng hoặc Ban Giám Hiệu chấm điểm và duyệt phiếu
     */
    public Nq52Evaluation reviewEvaluation(Long id, Nq52EvaluationForm form, User reviewer) {
        Nq52Evaluation eval = evaluationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu đánh giá ID: " + id));

        if (!canReview(reviewer, eval)) {
            throw new AccessDeniedException("Bạn không có quyền đánh giá phiếu của giáo viên này.");
        }

        if (eval.getStatus() == Nq52Status.DRAFT) {
            throw new IllegalStateException("Giáo viên chưa nộp phiếu, Tổ trưởng chưa thể đánh giá.");
        }

        // 1. Tính điểm Tổ trưởng Phần A (Tối đa 30) - Ràng buộc cứng không vượt quá thang điểm tối đa
        double scoreA = 0.0;
        Map<String, Double> reviewerScores = sanitizeAndClampScores(form.getReviewerScores());
        for (Map.Entry<String, Double> entry : Nq52CriteriaCatalog.MAX_SCORES_PART_A.entrySet()) {
            Double val = reviewerScores.get(entry.getKey());
            if (val != null && val > 0) {
                scoreA += val;
            }
        }
        scoreA = Math.min(scoreA, 30.0);
        eval.setReviewerScoreA(BigDecimal.valueOf(scoreA).setScale(2, RoundingMode.HALF_UP));

        // 2. Tính điểm Tổ trưởng Phần B (Tối đa 70)
        double scoreB = Nq52CriteriaCatalog.getScoreB1(form.getReviewerSelectedB1())
                      + Nq52CriteriaCatalog.getScoreB2(form.getReviewerSelectedB2())
                      + Nq52CriteriaCatalog.getScoreB3(form.getReviewerSelectedB3());
        scoreB = Math.min(scoreB, 70.0);
        eval.setReviewerScoreB(BigDecimal.valueOf(scoreB).setScale(2, RoundingMode.HALF_UP));

        // 3. Tính điểm Tổ trưởng Phần C (Điểm cộng & trừ)
        double scoreCPlus = 0.0;
        for (Map.Entry<String, Double> entry : Nq52CriteriaCatalog.MAX_SCORES_PART_C1.entrySet()) {
            Double val = reviewerScores.get(entry.getKey());
            if (val != null && val > 0) {
                scoreCPlus += Math.min(val, entry.getValue());
            }
        }
        scoreCPlus = Math.min(scoreCPlus, 6.0);

        double scoreCMinus = 0.0;
        Double cMinus1 = reviewerScores.get("C_II_1");
        if (cMinus1 != null && cMinus1 > 0) scoreCMinus += cMinus1;
        Double cMinus2 = reviewerScores.get("C_II_2");
        if (cMinus2 != null && cMinus2 > 0) scoreCMinus += cMinus2;
        Double cMinus3 = reviewerScores.get("C_II_3");
        if (cMinus3 != null && cMinus3 > 0) scoreCMinus += cMinus3;

        eval.setReviewerScoreCPlus(BigDecimal.valueOf(scoreCPlus).setScale(2, RoundingMode.HALF_UP));
        eval.setReviewerScoreCMinus(BigDecimal.valueOf(scoreCMinus).setScale(2, RoundingMode.HALF_UP));

        // Tổng điểm Tổ trưởng
        double total = scoreA + scoreB + scoreCPlus - scoreCMinus;
        if (total < 0) total = 0.0;
        eval.setReviewerTotalScore(BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP));

        // Xếp loại Tổ trưởng đề xuất
        if (form.getReviewerClassification() != null && !form.getReviewerClassification().isBlank()) {
            eval.setReviewerClassification(form.getReviewerClassification().trim());
        } else {
            eval.setReviewerClassification(Nq52CriteriaCatalog.determineClassification(eval.getReviewerTotalScore()));
        }

        eval.setReviewerNotes(form.getReviewerNotes());
        eval.setReviewer(reviewer);
        eval.setReviewerNameSnapshot(reviewer.getFullName());
        eval.setStatus(Nq52Status.REVIEWED);
        eval.setReviewedAt(Instant.now());

        // Cập nhật JSON
        Map<String, Object> data = parseJson(eval.getCriteriaDataJson());
        data.put("reviewerSelectedB1", form.getReviewerSelectedB1());
        data.put("reviewerSelectedB2", form.getReviewerSelectedB2());
        data.put("reviewerSelectedB3", form.getReviewerSelectedB3());
        data.put("reviewerScores", reviewerScores);
        try {
            eval.setCriteriaDataJson(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.error("Lỗi cập nhật JSON tiêu chí reviewer", e);
        }

        log.info("Tổ trưởng/BGH {} đã đánh giá xong phiếu NQ52 ID {}", reviewer.getEmail(), eval.getId());
        Nq52Evaluation saved = evaluationRepository.save(eval);

        // Tự động lưu bản file Word hoàn tất đánh giá vào thư mục tương ứng (nq52_totruongdanhgia / nq52_bangiamhieu)
        try {
            exportAndSaveDocx(saved.getId(), reviewer);
        } catch (Exception e) {
            log.warn("Không thể tự động xuất lưu file sau khi đánh giá: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Lấy danh sách phiếu giáo viên nộp cho Tổ trưởng / BGH duyệt
     * - BGH: Có thể xem toàn quyền ở 3 điểm trường hoặc lọc theo campusId, deptId.
     * - Tổ trưởng: Cố định chỉ xem được đúng tổ môn và đúng điểm trường / phân hiệu của mình.
     */
    @Transactional(readOnly = true)
    public List<Nq52Evaluation> getDepartmentEvaluations(User reviewer, Long filterCampusId, Long filterDepartmentId, Nq52Status filterStatus) {
        return getDepartmentEvaluations(reviewer, filterCampusId, filterDepartmentId, filterStatus, null, null);
    }

    @Transactional(readOnly = true)
    public List<Nq52Evaluation> getDepartmentEvaluations(User reviewer, Long filterCampusId, Long filterDepartmentId, Nq52Status filterStatus,
                                                         Integer filterMonth, Integer filterYear) {
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

        return evaluationRepository.findDepartmentEvaluationsFiltered(campusId, deptId, filterStatus, filterMonth, filterYear);
    }

    @Transactional(readOnly = true)
    public List<Nq52Evaluation> getDepartmentEvaluations(User reviewer, Long filterDepartmentId, Nq52Status filterStatus) {
        return getDepartmentEvaluations(reviewer, null, filterDepartmentId, filterStatus, null, null);
    }

    @Transactional(readOnly = true)
    public List<Nq52Evaluation> getDepartmentEvaluations(User reviewer, Long filterDepartmentId, Nq52Status filterStatus,
                                                         Integer filterMonth, Integer filterYear) {
        return getDepartmentEvaluations(reviewer, null, filterDepartmentId, filterStatus, filterMonth, filterYear);
    }

    /**
     * Nạp dữ liệu từ Entity sang DTO Form
     */
    public Nq52EvaluationForm populateForm(Nq52Evaluation eval) {
        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(eval.getId());
        form.setMonth(eval.getEvaluationMonth());
        form.setYear(eval.getEvaluationYear());
        form.setPositionTitle(eval.getPositionTitle());
        form.setWorkUnit(eval.getWorkUnit());
        form.setStrengths(eval.getStrengths());
        form.setWeaknesses(eval.getWeaknesses());
        form.setSelfClassification(eval.getSelfClassification());
        form.setReviewerNotes(eval.getReviewerNotes());
        form.setReviewerClassification(eval.getReviewerClassification());

        Map<String, Object> data = parseJson(eval.getCriteriaDataJson());
        form.setSelectedB1((String) data.get("selectedB1"));
        form.setSelectedB2((String) data.get("selectedB2"));
        form.setSelectedB3((String) data.get("selectedB3"));

        form.setReviewerSelectedB1((String) data.get("reviewerSelectedB1"));
        form.setReviewerSelectedB2((String) data.get("reviewerSelectedB2"));
        form.setReviewerSelectedB3((String) data.get("reviewerSelectedB3"));

        if (data.containsKey("selfScores")) {
            Map<String, Object> rawScores = (Map<String, Object>) data.get("selfScores");
            Map<String, Double> scores = new HashMap<>();
            rawScores.forEach((k, v) -> {
                if (v instanceof Number n) scores.put(k, n.doubleValue());
            });
            form.setSelfScores(scores);
        }

        if (data.containsKey("reviewerScores")) {
            Map<String, Object> rawScores = (Map<String, Object>) data.get("reviewerScores");
            Map<String, Double> scores = new HashMap<>();
            rawScores.forEach((k, v) -> {
                if (v instanceof Number n) scores.put(k, n.doubleValue());
            });
            form.setReviewerScores(scores);
        }

        if (data.containsKey("explanations")) {
            Map<String, String> exp = (Map<String, String>) data.get("explanations");
            form.setExplanations(exp);
        }

        return form;
    }

    public boolean canView(User user, Nq52Evaluation eval) {
        if (user == null || eval == null) return false;
        // Chủ sở hữu
        if (eval.getUser().getId().equals(user.getId())) return true;
        // Ban Giám Hiệu toàn quyền ở cả 3 điểm trường
        if (isAdmin(user)) return true;
        // Tổ trưởng của đúng tổ bộ môn và đúng điểm trường / phân hiệu
        return isDeptLeaderOf(user, eval);
    }

    public boolean canReview(User user, Nq52Evaluation eval) {
        if (user == null || eval == null) return false;
        // Ban Giám Hiệu toàn quyền xét duyệt ở cả 3 điểm trường
        if (isAdmin(user)) return true;
        // Tổ trưởng chỉ xét duyệt phiếu của giáo viên cùng tổ môn và cùng điểm trường / phân hiệu
        return isDeptLeaderOf(user, eval);
    }

    private static final String PERM_NQ52_REVIEW = "NQ52_REVIEW";

    public boolean isReviewer(User user) {
        if (user == null) return false;
        if (isAdmin(user)) return true;
        return hasRole(user, "DEPT_LEADER") || hasPermission(user, PERM_NQ52_REVIEW);
    }

    public boolean isAdmin(User user) {
        return hasRole(user, "ADMIN");
    }

    /**
     * Kiểm tra người dùng có phải là Tổ trưởng của đúng Tổ môn VÀ đúng Điểm trường / Phân hiệu của giáo viên không
     */
    public boolean isDeptLeaderOf(User user, Nq52Evaluation eval) {
        if (user == null || eval == null || user.getDepartment() == null || user.getCampus() == null) return false;
        boolean isLeader = hasRole(user, "DEPT_LEADER") || hasPermission(user, PERM_NQ52_REVIEW);
        if (!isLeader) return false;

        // 1. Phải cùng Tổ bộ môn
        boolean matchDept = eval.getDepartment() != null && user.getDepartment().getId().equals(eval.getDepartment().getId());

        // 2. Phải cùng Điểm trường / Phân hiệu
        boolean matchCampus = eval.getUser() != null && eval.getUser().getCampus() != null
                && user.getCampus().getId().equals(eval.getUser().getCampus().getId());

        return matchDept && matchCampus;
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

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Ràng buộc logic: Điểm từng tiêu chí không được vượt quá thang điểm tối đa theo cột mẫu
     * và không được nhỏ hơn 0.
     */
    private Map<String, Double> sanitizeAndClampScores(Map<String, Double> inputScores) {
        Map<String, Double> sanitized = new HashMap<>();
        if (inputScores == null) return sanitized;

        for (Map.Entry<String, Double> entry : inputScores.entrySet()) {
            String code = entry.getKey();
            Double val = entry.getValue();
            if (val == null) continue;

            if (val < 0) {
                val = 0.0;
            }

            // Tiêu chí Phần A
            if (Nq52CriteriaCatalog.MAX_SCORES_PART_A.containsKey(code)) {
                double max = Nq52CriteriaCatalog.MAX_SCORES_PART_A.get(code);
                if (val > max) {
                    log.warn("Điểm tiêu chí {} ({}) vượt quá thang tối đa ({}). Tự động khống chế về tối đa.", code, val, max);
                    val = max;
                }
            }
            // Tiêu chí Phần C.I (Điểm cộng)
            else if (Nq52CriteriaCatalog.MAX_SCORES_PART_C1.containsKey(code)) {
                double max = Nq52CriteriaCatalog.MAX_SCORES_PART_C1.get(code);
                if (val > max) {
                    log.warn("Điểm tiêu chí {} ({}) vượt quá thang tối đa ({}). Tự động khống chế về tối đa.", code, val, max);
                    val = max;
                }
            }

            sanitized.put(code, BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return sanitized;
    }

    /**
     * Xác định thư mục con tương đối dưới root uploads/ cho file NQ52
     * - nq52/nq52_canhandanhgia: Phiếu cá nhân giáo viên tự đánh giá
     * - nq52/nq52_totruongdanhgia: Phiếu do Tổ trưởng chuyên môn đánh giá
     * - nq52/nq52_bangiamhieu: Phiếu do Ban Giám Hiệu đánh giá
     */
    public String resolveStorageSubfolder(Nq52Evaluation eval, User currentUser) {
        if (eval.getStatus() == Nq52Status.REVIEWED) {
            boolean isBgh = isAdmin(currentUser) || (eval.getReviewer() != null && isAdmin(eval.getReviewer()));
            return isBgh ? DIR_BAN_GIAM_HIEU : DIR_TO_TRUONG;
        }
        return DIR_CAN_NHAN;
    }

    /**
     * Đường dẫn thư mục lưu trữ file excel tự đánh giá kết quả (sẵn sàng scale)
     */
    public Path getExcelResultsStoragePath() {
        if (appProperties != null && appProperties.getStorage() != null && appProperties.getStorage().getRoot() != null) {
            return Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize().resolve(DIR_TU_DANH_GIA_KET_QUA);
        }
        return Paths.get("uploads").toAbsolutePath().normalize().resolve(DIR_TU_DANH_GIA_KET_QUA);
    }

    /**
     * Xuất file Word Mẫu 03 (.docx) và lưu vào database (cả binary và metadata),
     * đồng thời phân loại lưu trữ theo đúng thư mục:
     * - uploads/nq52/nq52_canhandanhgia
     * - uploads/nq52/nq52_totruongdanhgia
     * - uploads/nq52/nq52_bangiamhieu
     */
    public Nq52Evaluation exportAndSaveDocx(Long id, User currentUser) {
        Nq52Evaluation eval = getEvaluationForView(id, currentUser);
        Nq52EvaluationForm form = populateForm(eval);

        byte[] docxBytes = docxExportService.generateM03Docx(eval, form);

        String cleanName = removeAccents(eval.getTeacherNameSnapshot()).replaceAll("[^a-zA-Z0-9_]", "_");
        String filename = "Phieu_Danh_Gia_NQ52_Thang_" + eval.getEvaluationMonth() + "_" + eval.getEvaluationYear() + "_" + cleanName + ".docx";

        eval.setExportedFileName(filename);
        eval.setExportedFileSize((long) docxBytes.length);
        eval.setExportedFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        eval.setExportedAt(Instant.now());
        eval.setExportedFileData(docxBytes);

        // Lưu bản sao trên hệ thống lưu trữ tệp theo cấu trúc thư mục con NQ52
        try {
            if (appProperties != null && appProperties.getStorage() != null && appProperties.getStorage().getRoot() != null) {
                Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
                String subfolder = resolveStorageSubfolder(eval, currentUser);
                Path targetDir = rootPath.resolve(subfolder);
                Files.createDirectories(targetDir);

                String storedFilename = UUID.randomUUID().toString() + ".docx";
                Path targetPath = targetDir.resolve(storedFilename);
                Files.write(targetPath, docxBytes);

                // Lưu đường dẫn tương đối từ thư mục root (e.g. nq52/nq52_canhandanhgia/uuid.docx)
                eval.setExportedFilePath(subfolder + "/" + storedFilename);
                log.info("Đã lưu file Word NQ52 vào đĩa: {}/{}", subfolder, storedFilename);
            }
        } catch (Exception e) {
            log.warn("Không thể lưu bản sao file lên đĩa (đã lưu an toàn trong database): {}", e.getMessage());
        }

        return evaluationRepository.save(eval);
    }

    /**
     * Lấy dữ liệu file đã xuất lưu trong CSDL hoặc đọc từ file vật lý
     */
    @Transactional(readOnly = true)
    public byte[] getExportedFileData(Long id, User currentUser) {
        Nq52Evaluation eval = getEvaluationForView(id, currentUser);
        if (eval.getExportedFileData() != null && eval.getExportedFileData().length > 0) {
            return eval.getExportedFileData();
        }
        // Thử đọc từ đĩa nếu có filePath
        if (eval.getExportedFilePath() != null && appProperties != null && appProperties.getStorage() != null) {
            try {
                Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
                Path targetPath = rootPath.resolve(eval.getExportedFilePath());
                if (Files.exists(targetPath)) {
                    return Files.readAllBytes(targetPath);
                }
                // Hỗ trợ ngược file cũ lưu ở rootPath
                Path legacyPath = rootPath.resolve(Paths.get(eval.getExportedFilePath()).getFileName().toString());
                if (Files.exists(legacyPath)) {
                    return Files.readAllBytes(legacyPath);
                }
            } catch (Exception e) {
                log.error("Không thể đọc file từ đĩa: {}", eval.getExportedFilePath(), e);
            }
        }
        throw new IllegalStateException("Phiếu này chưa được xuất file hoặc file chưa được lưu trong cơ sở dữ liệu.");
    }

    private String removeAccents(String text) {
        if (text == null) return "Giao_Vien";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
}

