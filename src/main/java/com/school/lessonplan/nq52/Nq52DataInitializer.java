package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.signature.Nq52DigitalSignatureRepository;
import com.school.lessonplan.nq52.signature.Nq52UserSignatureProfileRepository;
import com.school.lessonplan.role.Permission;
import com.school.lessonplan.role.PermissionRepository;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.role.RoleRepository;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(10)
public class Nq52DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Nq52DataInitializer.class);

    private final UserRepository userRepository;
    private final Nq52EvaluationRepository evaluationRepository;
    private final Nq52WorkResultRepository workResultRepository;
    private final Nq52DigitalSignatureRepository digitalSignatureRepository;
    private final Nq52UserSignatureProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public Nq52DataInitializer(UserRepository userRepository,
                               Nq52EvaluationRepository evaluationRepository,
                               Nq52WorkResultRepository workResultRepository,
                               Nq52DigitalSignatureRepository digitalSignatureRepository,
                               Nq52UserSignatureProfileRepository profileRepository,
                               RoleRepository roleRepository,
                               PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.evaluationRepository = evaluationRepository;
        this.workResultRepository = workResultRepository;
        this.digitalSignatureRepository = digitalSignatureRepository;
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initPermissions();
        cleanupAllMockData();
    }

    private void initPermissions() {
        try {
            Permission accessPerm = permissionRepository.findByCode(Permission.NQ52_ACCESS)
                    .orElseGet(() -> permissionRepository.save(new Permission(Permission.NQ52_ACCESS)));
            Permission reviewPerm = permissionRepository.findByCode(Permission.NQ52_REVIEW)
                    .orElseGet(() -> permissionRepository.save(new Permission(Permission.NQ52_REVIEW)));

            roleRepository.findByCode("ADMIN").ifPresent(adminRole -> {
                boolean changed = false;
                if (adminRole.getPermissions() == null) {
                    adminRole.setPermissions(new java.util.HashSet<>());
                }
                if (!adminRole.getPermissions().contains(accessPerm)) {
                    adminRole.getPermissions().add(accessPerm);
                    changed = true;
                }
                if (!adminRole.getPermissions().contains(reviewPerm)) {
                    adminRole.getPermissions().add(reviewPerm);
                    changed = true;
                }
                if (changed) {
                    roleRepository.save(adminRole);
                    log.info("Đã gán quyền NQ52_ACCESS và NQ52_REVIEW cho vai trò ADMIN.");
                }
            });
        } catch (Exception ex) {
            log.warn("Lỗi khi khởi tạo quyền NQ52: {}", ex.getMessage());
        }
    }

    private void cleanupAllMockData() {
        log.info("Bắt đầu dọn dẹp toàn bộ dữ liệu mock của module Nghị quyết 52...");

        // 1. Xóa các tài khoản giáo viên giả lập chỉ dùng để seed mock
        List<String> mockEmails = List.of(
                "totruong.ph1@school.edu.vn",
                "gv.ph1@school.edu.vn",
                "gv.ph2@school.edu.vn",
                "gv.mai@school.edu.vn"
        );

        for (String email : mockEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                try {
                    // Xóa chữ ký số gắn với tài liệu của user này
                    evaluationRepository.findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(user.getId()).forEach(e -> {
                        digitalSignatureRepository.deleteAll(digitalSignatureRepository.findByDocumentTypeAndDocumentIdOrderBySignedAtAsc("EVALUATION", e.getId()));
                    });
                    workResultRepository.findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(user.getId()).forEach(w -> {
                        digitalSignatureRepository.deleteAll(digitalSignatureRepository.findByDocumentTypeAndDocumentIdOrderBySignedAtAsc("WORK_RESULT", w.getId()));
                    });

                    // Xóa phiếu đánh giá và bảng kết quả
                    evaluationRepository.deleteAll(evaluationRepository.findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(user.getId()));
                    workResultRepository.deleteAll(workResultRepository.findByUserIdOrderByEvaluationYearDescEvaluationMonthDesc(user.getId()));

                    // Xóa hồ sơ chữ ký số nếu có
                    profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);

                    // Xóa user
                    userRepository.delete(user);
                    log.info("Đã xóa mock user: {}", email);
                } catch (Exception ex) {
                    log.warn("Không thể xóa mock user {}: {}", email, ex.getMessage());
                }
            });
        }


        // 2. Làm sạch các hồ sơ chữ ký số (profiles) còn lưu dữ liệu mock cũ trong database
        try {
            profileRepository.findAll().forEach(p -> {
                boolean dirty = false;
                if ("048085001234".equals(p.getNationalId()) || "048085001234".equals(p.getPersonalId())) {
                    p.setNationalId("");
                    p.setPersonalId("");
                    dirty = true;
                }
                if ("15/05/1985".equals(p.getDob())) {
                    p.setDob("");
                    dirty = true;
                }
                if (p.getMoetId() != null && p.getMoetId().startsWith("4801123456")) {
                    p.setMoetId("");
                    dirty = true;
                }
                // Nếu hồ sơ có certSerial là SIM hoặc MOCK hoặc chưa liên kết thật
                if (p.getCertSerial() != null && (p.getCertSerial().contains("SIM") || p.getCertSerial().contains("MOCK"))) {
                    p.setCertSerial("");
                    p.setCertSubject("");
                    p.setValidFrom("");
                    p.setValidTo("");
                    p.setOwnerInfo("");
                    p.setRegistrationStatus("Chưa liên kết");
                    p.setLinkedAt(null);
                    p.setSignatureImageBase64("");
                    dirty = true;
                }
                if (dirty) {
                    profileRepository.save(p);
                    log.info("Đã làm sạch dữ liệu mock trong hồ sơ chữ ký số của user id={}", p.getUser() != null ? p.getUser().getId() : null);
                }
            });
            log.info("Hoàn tất dọn dẹp profile mock trong database.");
        } catch (Exception ex) {
            log.warn("Lỗi cleanup profile: {}", ex.getMessage());
        }

        // 3. Xóa các chữ ký số giả lập còn sót lại trong bảng nq52_digital_signatures
        try {
            digitalSignatureRepository.findAll().forEach(sig -> {
                if (sig.getCertSerial() != null && (sig.getCertSerial().contains("SIM") || sig.getCertSerial().contains("MOCK") || sig.getCertSerial().startsWith("77:88:99:AA"))) {
                    digitalSignatureRepository.delete(sig);
                    log.info("Đã xóa chữ ký số mock id={}", sig.getId());
                }
            });
        } catch (Exception ex) {
            log.warn("Lỗi cleanup signatures: {}", ex.getMessage());
        }

        log.info("Hoàn tất dọn dẹp toàn bộ dữ liệu mock của module Nghị quyết 52.");
    }
}
