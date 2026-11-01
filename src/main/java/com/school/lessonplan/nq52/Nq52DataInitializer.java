package com.school.lessonplan.nq52;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.campus.Campus;
import com.school.lessonplan.campus.CampusRepository;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import com.school.lessonplan.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(10)
public class Nq52DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Nq52DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final PasswordEncoder passwordEncoder;
    private final Nq52EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final Nq52WorkResultService workResultService;

    public Nq52DataInitializer(UserRepository userRepository,
                               RoleRepository roleRepository,
                               DepartmentRepository departmentRepository,
                               CampusRepository campusRepository,
                               PasswordEncoder passwordEncoder,
                               Nq52EvaluationRepository evaluationRepository,
                               ObjectMapper objectMapper,
                               Nq52WorkResultService workResultService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.campusRepository = campusRepository;
        this.passwordEncoder = passwordEncoder;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
        this.workResultService = workResultService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedSampleTeachersAndEvaluations();
    }

    private void seedSampleTeachersAndEvaluations() {
        Role teacherRole = roleRepository.findByCode("TEACHER").orElse(null);
        Role leaderRole = roleRepository.findByCode("DEPT_LEADER").orElse(teacherRole);

        Department toanDept = departmentRepository.findAll().stream()
                .filter(d -> d.getName() != null && d.getName().toLowerCase().contains("toán"))
                .findFirst()
                .orElseGet(() -> departmentRepository.findAll().stream().findFirst().orElse(null));

        Department vanDept = departmentRepository.findAll().stream()
                .filter(d -> d.getName() != null && d.getName().toLowerCase().contains("văn"))
                .findFirst()
                .orElse(toanDept);

        Campus defaultCampus = campusRepository.findByIsDefaultTrue()
                .orElseGet(() -> campusRepository.findAll().stream().findFirst().orElse(null));

        String defaultTeacherPassword = "Password123!";
        String encodedTeacherPassword = passwordEncoder.encode(defaultTeacherPassword);

        // 1. Tạo Tổ trưởng môn Toán: Lê Hoàng Long (totruong@school.edu.vn)
        User leaderLong = userRepository.findByEmail("totruong@school.edu.vn").orElseGet(() -> {
            User u = new User();
            u.setLastName("Lê Hoàng");
            u.setGivenName("Long");
            u.setEmail("totruong@school.edu.vn");
            u.setPasswordHash(encodedTeacherPassword);
            u.setStatus(UserStatus.ACTIVE);
            u.setDepartment(toanDept);
            u.setCampus(defaultCampus);
            if (leaderRole != null) u.getRoles().add(leaderRole);
            if (teacherRole != null) u.getRoles().add(teacherRole);
            User saved = userRepository.save(u);
            log.info("Sample Team Leader created: {} (Password: {})", saved.getEmail(), defaultTeacherPassword);
            return saved;
        });

        // 2. Tạo Giáo viên môn Toán: Nguyễn Văn An (giaovien@school.edu.vn)
        User teacherAn = userRepository.findByEmail("giaovien@school.edu.vn").orElseGet(() -> {
            User u = new User();
            u.setLastName("Nguyễn Văn");
            u.setGivenName("An");
            u.setEmail("giaovien@school.edu.vn");
            u.setPasswordHash(encodedTeacherPassword);
            u.setStatus(UserStatus.ACTIVE);
            u.setDepartment(toanDept);
            u.setCampus(defaultCampus);
            if (teacherRole != null) u.getRoles().add(teacherRole);
            User saved = userRepository.save(u);
            log.info("Sample Teacher created: {} (Password: {})", saved.getEmail(), defaultTeacherPassword);
            return saved;
        });

        // 3. Tạo Giáo viên môn Văn: Trần Thị Mai (gv.mai@school.edu.vn)
        User teacherMai = userRepository.findByEmail("gv.mai@school.edu.vn").orElseGet(() -> {
            User u = new User();
            u.setLastName("Trần Thị");
            u.setGivenName("Mai");
            u.setEmail("gv.mai@school.edu.vn");
            u.setPasswordHash(encodedTeacherPassword);
            u.setStatus(UserStatus.ACTIVE);
            u.setDepartment(vanDept);
            u.setCampus(defaultCampus);
            if (teacherRole != null) u.getRoles().add(teacherRole);
            User saved = userRepository.save(u);
            log.info("Sample Teacher created: {} (Password: {})", saved.getEmail(), defaultTeacherPassword);
            return saved;
        });

        // 4. Khởi tạo các phiếu mẫu NQ52 cho Nguyễn Văn An
        seedEvaluation(teacherAn, 10, 2026, Nq52Status.DRAFT, 28.0, 65.0, 2.0, 0.0, 95.0,
                "Hoàn thành xuất sắc nhiệm vụ",
                "Nhiệt tình trong công tác giảng dạy, hoàn thành tốt các tiết thao giảng và ứng dụng CNTT.",
                "Cần dành thêm thời gian bồi dưỡng học sinh mũi nhọn.",
                null, null, null, null, null, null, null, null);

        seedEvaluation(teacherAn, 9, 2026, Nq52Status.SUBMITTED, 27.0, 60.0, 1.0, 0.0, 88.0,
                "Hoàn thành tốt nhiệm vụ",
                "Chấp hành tốt nội quy nhà trường, tích cực tham gia các phong trào thi đua.",
                "Cần tích cực đổi mới phương pháp kiểm tra đánh giá.",
                null, null, null, null, null, null, null, null);

        seedEvaluation(teacherAn, 8, 2026, Nq52Status.REVIEWED, 28.0, 62.0, 2.0, 0.0, 92.0,
                "Hoàn thành xuất sắc nhiệm vụ",
                "Hoàn thành xuất sắc nhiệm vụ đầu năm học, chuẩn bị kế hoạch bài dạy chỉn chu.",
                "Không có khuyết điểm lớn.",
                28.0, 60.0, 2.0, 0.0, 90.0,
                "Hoàn thành xuất sắc nhiệm vụ",
                "Đồng chí Nguyễn Văn An thực hiện nghiêm túc quy chế chuyên môn, bài giảng chuẩn bị kỹ lưỡng, hoàn thành xuất sắc nhiệm vụ.",
                leaderLong);

        // 5. Khởi tạo phiếu mẫu cho Trần Thị Mai (Tháng 10/2026 - SUBMITTED)
        seedEvaluation(teacherMai, 10, 2026, Nq52Status.SUBMITTED, 27.5, 60.0, 1.5, 0.0, 89.0,
                "Hoàn thành tốt nhiệm vụ",
                "Tích cực hướng dẫn học sinh đọc sách, văn hóa đọc tiến bộ rõ rệt.",
                "Cần phối hợp chặt chẽ hơn với giáo viên chủ nhiệm.",
                null, null, null, null, null, null, null, null);

        Campus ph1 = campusRepository.findByName("Phân hiệu 1").orElse(null);
        Campus ph2 = campusRepository.findByName("Phân hiệu 2").orElse(null);

        // 6. Tạo Tổ trưởng & Giáo viên tại Phân hiệu 1
        if (ph1 != null) {
            userRepository.findByEmail("totruong.ph1@school.edu.vn").orElseGet(() -> {
                User u = new User();
                u.setLastName("Hoàng Văn");
                u.setGivenName("Đức");
                u.setEmail("totruong.ph1@school.edu.vn");
                u.setPasswordHash(encodedTeacherPassword);
                u.setStatus(UserStatus.ACTIVE);
                u.setDepartment(toanDept);
                u.setCampus(ph1);
                if (leaderRole != null) u.getRoles().add(leaderRole);
                if (teacherRole != null) u.getRoles().add(teacherRole);
                return userRepository.save(u);
            });

            User teacherPh1 = userRepository.findByEmail("gv.ph1@school.edu.vn").orElseGet(() -> {
                User u = new User();
                u.setLastName("Phạm Thị");
                u.setGivenName("Lan");
                u.setEmail("gv.ph1@school.edu.vn");
                u.setPasswordHash(encodedTeacherPassword);
                u.setStatus(UserStatus.ACTIVE);
                u.setDepartment(toanDept);
                u.setCampus(ph1);
                if (teacherRole != null) u.getRoles().add(teacherRole);
                return userRepository.save(u);
            });

            seedEvaluation(teacherPh1, 10, 2026, Nq52Status.SUBMITTED, 28.0, 62.0, 2.0, 0.0, 92.0,
                    "Hoàn thành xuất sắc nhiệm vụ",
                    "Giáo viên cốt cán tại Phân hiệu 1, phụ trách bồi dưỡng học sinh giỏi môn Toán.",
                    "Cần sắp xếp thời gian hợp lý hơn giữa giảng dạy và công tác đoàn thể.",
                    null, null, null, null, null, null, null, null);
        }

        // 7. Tạo Giáo viên tại Phân hiệu 2
        if (ph2 != null) {
            User teacherPh2 = userRepository.findByEmail("gv.ph2@school.edu.vn").orElseGet(() -> {
                User u = new User();
                u.setLastName("Vũ Minh");
                u.setGivenName("Tuấn");
                u.setEmail("gv.ph2@school.edu.vn");
                u.setPasswordHash(encodedTeacherPassword);
                u.setStatus(UserStatus.ACTIVE);
                u.setDepartment(toanDept);
                u.setCampus(ph2);
                if (teacherRole != null) u.getRoles().add(teacherRole);
                return userRepository.save(u);
            });

            seedEvaluation(teacherPh2, 10, 2026, Nq52Status.SUBMITTED, 26.5, 58.0, 1.0, 0.0, 85.5,
                    "Hoàn thành tốt nhiệm vụ",
                    "Hoàn thành tốt nhiệm vụ giảng dạy được phân công tại Phân hiệu 2.",
                    "Cần tăng cường ứng dụng công nghệ thông tin trong bài giảng.",
                    null, null, null, null, null, null, null, null);
        }

        // Đảm bảo tất cả các bản nháp (DRAFT) của giáo viên Nguyễn Văn An đều để trống điểm để tự điền kiểm thử
        evaluationRepository.findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(teacherAn.getId()).forEach(e -> {
            if (e.getStatus() == Nq52Status.DRAFT) {
                e.setSelfScoreA(BigDecimal.ZERO);
                e.setSelfScoreB(BigDecimal.ZERO);
                e.setSelfScoreCPlus(BigDecimal.ZERO);
                e.setSelfScoreCMinus(BigDecimal.ZERO);
                e.setSelfTotalScore(BigDecimal.ZERO);
                e.setSelfClassification(null);
                e.setStrengths(null);
                e.setWeaknesses(null);
                Map<String, Object> blankData = new HashMap<>();
                blankData.put("selfScores", new HashMap<String, Double>());
                blankData.put("explanations", new HashMap<String, String>());
                try {
                    e.setCriteriaDataJson(objectMapper.writeValueAsString(blankData));
                } catch (Exception ex) {}
                evaluationRepository.save(e);
            }
        });

        // 8. Khởi tạo Bảng tự đánh giá kết quả (Mẫu 01TKNV) mẫu cho giáo viên Nguyễn Văn An
        try {
            workResultService.getOrCreateWorkResult(teacherAn, 10, 2026);
            Nq52WorkResult wr9 = workResultService.getOrCreateWorkResult(teacherAn, 9, 2026);
            if (wr9.getStatus() == Nq52Status.DRAFT) {
                wr9.setStatus(Nq52Status.SUBMITTED);
                wr9.setSubmittedAt(Instant.now());
            }
        } catch (Exception e) {
            log.warn("Không thể seed mẫu work results: {}", e.getMessage());
        }
    }

    private void seedEvaluation(User user, int month, int year, Nq52Status status,
                                Double selfA, Double selfB, Double selfCPlus, Double selfCMinus, Double selfTotal,
                                String selfClassification, String strengths, String weaknesses,
                                Double revA, Double revB, Double revCPlus, Double revCMinus, Double revTotal,
                                String revClassification, String revNotes, User reviewer) {
        var existingOpt = evaluationRepository.findByUserIdAndEvaluationMonthAndEvaluationYear(user.getId(), month, year);
        if (existingOpt.isPresent()) {
            Nq52Evaluation existing = existingOpt.get();
            // Đảm bảo Tháng 10/2026 của giáo viên Nguyễn Văn An luôn là bản nháp sạch để người dùng kiểm thử
            if (month == 10 && year == 2026 && "giaovien@school.edu.vn".equalsIgnoreCase(user.getEmail())) {
                existing.setStatus(Nq52Status.DRAFT);
                existing.setSubmittedAt(null);
                existing.setReviewedAt(null);
                existing.setReviewer(null);
                existing.setReviewerTotalScore(null);
                existing.setReviewerClassification(null);
                existing.setReviewerNotes(null);
                existing.setSelfScoreA(BigDecimal.ZERO);
                existing.setSelfScoreB(BigDecimal.ZERO);
                existing.setSelfScoreCPlus(BigDecimal.ZERO);
                existing.setSelfScoreCMinus(BigDecimal.ZERO);
                existing.setSelfTotalScore(BigDecimal.ZERO);
                existing.setSelfClassification(null);
                existing.setStrengths(null);
                existing.setWeaknesses(null);
                Map<String, Object> blankData = new HashMap<>();
                blankData.put("selfScores", new HashMap<String, Double>());
                blankData.put("explanations", new HashMap<String, String>());
                try {
                    existing.setCriteriaDataJson(objectMapper.writeValueAsString(blankData));
                } catch (Exception e) {
                    existing.setCriteriaDataJson("{}");
                }
                evaluationRepository.save(existing);
                log.info("Reset NQ52 Month 10/2026 to fresh DRAFT for {}", user.getEmail());
                return;
            }
            return;
        }

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setUser(user);
        eval.setDepartment(user.getDepartment());
        eval.setEvaluationMonth(month);
        eval.setEvaluationYear(year);
        eval.setStatus(status);
        eval.setTeacherNameSnapshot(user.getLastName() + " " + user.getGivenName());
        eval.setPositionTitle("Giáo viên");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setDepartmentNameSnapshot(user.getDepartment() != null ? user.getDepartment().getName() : "Toán học");

        if (status == Nq52Status.DRAFT) {
            eval.setSelfScoreA(BigDecimal.ZERO);
            eval.setSelfScoreB(BigDecimal.ZERO);
            eval.setSelfScoreCPlus(BigDecimal.ZERO);
            eval.setSelfScoreCMinus(BigDecimal.ZERO);
            eval.setSelfTotalScore(BigDecimal.ZERO);
            eval.setSelfClassification(null);
            eval.setStrengths(null);
            eval.setWeaknesses(null);
            Map<String, Object> blankData = new HashMap<>();
            blankData.put("selfScores", new HashMap<String, Double>());
            blankData.put("explanations", new HashMap<String, String>());
            try {
                eval.setCriteriaDataJson(objectMapper.writeValueAsString(blankData));
            } catch (Exception e) {
                eval.setCriteriaDataJson("{}");
            }
            evaluationRepository.save(eval);
            log.info("Sample NQ52 empty DRAFT created for {} - Month {}/{}", user.getEmail(), month, year);
            return;
        }

        eval.setSelfScoreA(BigDecimal.valueOf(selfA).setScale(2, RoundingMode.HALF_UP));
        eval.setSelfScoreB(BigDecimal.valueOf(selfB).setScale(2, RoundingMode.HALF_UP));
        eval.setSelfScoreCPlus(BigDecimal.valueOf(selfCPlus).setScale(2, RoundingMode.HALF_UP));
        eval.setSelfScoreCMinus(BigDecimal.valueOf(selfCMinus).setScale(2, RoundingMode.HALF_UP));
        eval.setSelfTotalScore(BigDecimal.valueOf(selfTotal).setScale(2, RoundingMode.HALF_UP));
        eval.setSelfClassification(selfClassification);
        eval.setStrengths(strengths);
        eval.setWeaknesses(weaknesses);

        Map<String, Object> data = new HashMap<>();
        data.put("selectedB1", "a");
        data.put("selectedB2", "a");
        data.put("selectedB3", "a");
        Map<String, Double> selfScores = new HashMap<>();
        selfScores.put("A_I_1", 3.0);
        selfScores.put("A_I_2", 3.0);
        selfScores.put("A_I_3", 2.0);
        selfScores.put("A_I_4", 2.0);
        selfScores.put("A_II_1", 2.0);
        selfScores.put("A_II_2", 2.0);
        selfScores.put("A_II_3", 3.0);
        selfScores.put("A_II_4", 3.0);
        selfScores.put("A_III_1", 2.0);
        selfScores.put("A_III_2", 2.0);
        selfScores.put("A_III_3", 2.0);
        selfScores.put("A_III_4", 2.0);
        data.put("selfScores", selfScores);

        if (status == Nq52Status.SUBMITTED || status == Nq52Status.REVIEWED) {
            eval.setSubmittedAt(Instant.now());
        }

        if (status == Nq52Status.REVIEWED && revTotal != null) {
            eval.setReviewerScoreA(BigDecimal.valueOf(revA).setScale(2, RoundingMode.HALF_UP));
            eval.setReviewerScoreB(BigDecimal.valueOf(revB).setScale(2, RoundingMode.HALF_UP));
            eval.setReviewerScoreCPlus(BigDecimal.valueOf(revCPlus).setScale(2, RoundingMode.HALF_UP));
            eval.setReviewerScoreCMinus(BigDecimal.valueOf(revCMinus).setScale(2, RoundingMode.HALF_UP));
            eval.setReviewerTotalScore(BigDecimal.valueOf(revTotal).setScale(2, RoundingMode.HALF_UP));
            eval.setReviewerClassification(revClassification);
            eval.setReviewerNotes(revNotes);
            eval.setReviewer(reviewer);
            eval.setReviewerNameSnapshot(reviewer != null ? (reviewer.getLastName() + " " + reviewer.getGivenName()) : "Tổ trưởng chuyên môn");
            eval.setReviewedAt(Instant.now());

            data.put("reviewerSelectedB1", "a");
            data.put("reviewerSelectedB2", "a");
            data.put("reviewerSelectedB3", "a");
            data.put("reviewerScores", selfScores);
        }

        try {
            eval.setCriteriaDataJson(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            eval.setCriteriaDataJson("{}");
        }

        evaluationRepository.save(eval);
        log.info("Sample NQ52 Evaluation created for {} - Month {}/{} - Status {}",
                user.getEmail(), month, year, status);
    }
}

