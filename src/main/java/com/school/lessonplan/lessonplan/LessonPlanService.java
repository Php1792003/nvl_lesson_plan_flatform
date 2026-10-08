package com.school.lessonplan.lessonplan;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.department.DepartmentRepository;
import com.school.lessonplan.evaluation.EvaluationRepository;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
public class LessonPlanService {

    private static final Logger log = LoggerFactory.getLogger(LessonPlanService.class);

    private final LessonPlanRepository lessonPlanRepository;
    private final LessonPlanVersionRepository versionRepository;
    private final EvaluationRepository evaluationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final LessonPlanAccessService accessService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public LessonPlanService(LessonPlanRepository lessonPlanRepository,
                             LessonPlanVersionRepository versionRepository,
                             EvaluationRepository evaluationRepository,
                             DepartmentRepository departmentRepository,
                             UserRepository userRepository,
                             LessonPlanAccessService accessService,
                             FileStorageService fileStorageService,
                             AuditService auditService,
                             AppProperties appProperties) {
        this.lessonPlanRepository = lessonPlanRepository;
        this.versionRepository = versionRepository;
        this.evaluationRepository = evaluationRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    public Page<LessonPlan> getAccessiblePlans(CurrentUser user, Pageable pageable) {
        boolean isAdmin = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return lessonPlanRepository.findAll(pageable);
        }
        return lessonPlanRepository.findAccessibleByUser(user.getId(), pageable);
    }

    public Page<LessonPlan> getOrphanPlans(Pageable pageable) {
        return lessonPlanRepository.findByOwnerIsNull(pageable);
    }

    public Page<LessonPlan> searchAllForAdmin(String search, Long departmentId, String schoolYear,
                                             Integer semester, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return lessonPlanRepository.searchAll(cleanSearch, departmentId, schoolYear, semester, pageable);
    }

    public LessonPlan getPlanById(Long id, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));
        accessService.requireViewAccess(user, plan);
        auditService.log("PLAN_VIEW", "LESSON_PLAN", plan.getId(), "Xem chi tiết giáo án: " + plan.getTitle());
        return plan;
    }

    @Transactional
    public LessonPlan createPlan(CurrentUser user, String title, Long departmentId,
                                 String grade, String schoolYear, Integer semester,
                                 Integer weekNo, String description, MultipartFile file) {
        User owner = userRepository.findById(user.getId())
            .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng."));

        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));

        // Lưu file vật lý
        FileStorageService.StoredFileResult stored = fileStorageService.storeFile(file);

        // Tạo LessonPlan
        LessonPlan plan = new LessonPlan();
        plan.setOwner(owner);
        plan.setTitle(title.trim());
        plan.setDepartment(department);
        plan.setGrade(grade.trim());
        plan.setSchoolYear(schoolYear.trim());
        plan.setSemester(semester);
        plan.setWeekNo(weekNo);
        plan.setDescription(description != null ? description.trim() : null);
        plan.setCurrentVersionNo(1);
        plan = lessonPlanRepository.save(plan);

        // Tạo Version 1
        LessonPlanVersion version = new LessonPlanVersion();
        version.setPlan(plan);
        version.setVersionNo(1);
        version.setOriginalFilename(stored.originalFilename());
        version.setStoredFilename(stored.storedFilename());
        version.setContentType(stored.contentType());
        version.setSizeBytes(stored.sizeBytes());
        version.setSha256(stored.sha256());
        versionRepository.save(version);

        auditService.log("PLAN_CREATE", "LESSON_PLAN", plan.getId(), "Tạo giáo án: " + plan.getTitle());
        return plan;
    }

    @Transactional
    public LessonPlanVersion addVersion(Long planId, CurrentUser user, MultipartFile file) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireManageAccess(user, plan);

        // Upload file và tính sha256
        FileStorageService.StoredFileResult stored = fileStorageService.storeFile(file);

        // Kiểm tra chống trùng với version hiện tại
        if (versionRepository.existsCurrentVersionWithSha256(planId, stored.sha256())) {
            fileStorageService.deletePhysicalFile(stored.storedFilename());
            throw new BusinessException("File giống bản hiện tại. Vui lòng tải lên nội dung mới.");
        }

        int newVersionNo = plan.getCurrentVersionNo() + 1;
        plan.setCurrentVersionNo(newVersionNo);
        lessonPlanRepository.save(plan);

        LessonPlanVersion version = new LessonPlanVersion();
        version.setPlan(plan);
        version.setVersionNo(newVersionNo);
        version.setOriginalFilename(stored.originalFilename());
        version.setStoredFilename(stored.storedFilename());
        version.setContentType(stored.contentType());
        version.setSizeBytes(stored.sizeBytes());
        version.setSha256(stored.sha256());
        version = versionRepository.save(version);

        auditService.log("PLAN_NEW_VERSION", "LESSON_PLAN", plan.getId(),
            "Tải lên phiên bản " + newVersionNo + " cho giáo án: " + plan.getTitle());

        return version;
    }

    @Transactional
    public LessonPlan updateMetadata(Long planId, CurrentUser user, String title,
                                     Long departmentId, String grade, String schoolYear,
                                     Integer semester, Integer weekNo, String description) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireManageAccess(user, plan);

        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));

        plan.setTitle(title.trim());
        plan.setDepartment(department);
        plan.setGrade(grade.trim());
        plan.setSchoolYear(schoolYear.trim());
        plan.setSemester(semester);
        plan.setWeekNo(weekNo);
        plan.setDescription(description != null ? description.trim() : null);

        plan = lessonPlanRepository.save(plan);
        auditService.log("PLAN_UPDATE", "LESSON_PLAN", plan.getId(), "Cập nhật thông tin giáo án: " + plan.getTitle());
        return plan;
    }

    /**
     * Quy tắc 4.5b: Giáo án đã có đánh giá thì KHÔNG ĐƯỢC XOÁ.
     * Giáo án chưa có đánh giá được xoá thật (dòng DB + files trên đĩa).
     */
    @Transactional
    public void deletePlan(Long planId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireManageAccess(user, plan);

        // Kiểm tra đánh giá
        if (evaluationRepository.existsByPlanId(planId)) {
            throw new BusinessException("Giáo án đã có nhận xét/đánh giá từ Ban Giám Hiệu, không thể xoá. Thầy/Cô chỉ có thể tải lên phiên bản mới.");
        }

        // Xoá file vật lý của tất cả versions
        List<LessonPlanVersion> versions = versionRepository.findAllByPlanId(planId);
        for (LessonPlanVersion v : versions) {
            fileStorageService.deletePhysicalFile(v.getStoredFilename());
        }

        auditService.log("PLAN_DELETE", "LESSON_PLAN", plan.getId(), "Xoá giáo án: " + plan.getTitle());
        lessonPlanRepository.delete(plan);
    }

    public DownloadResult downloadVersion(Long planId, Long versionId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireViewAccess(user, plan);

        LessonPlanVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản giáo án."));

        if (!version.getPlan().getId().equals(planId)) {
            throw new ResourceNotFoundException("Phiên bản không thuộc giáo án này.");
        }

        // Kiểm tra xem file đã bị dọn (purge) chưa theo mục 4.7
        if (version.getFilePurgedAt() != null) {
            throw new BusinessException("File đã được dọn để tiết kiệm dung lượng. Phiên bản này chỉ còn lưu hồ sơ thông tin.");
        }

        Resource resource = fileStorageService.loadFileAsResource(version.getStoredFilename());

        auditService.log("PLAN_DOWNLOAD", "LESSON_PLAN", plan.getId(),
            "Tải xuống phiên bản " + version.getVersionNo() + ": " + version.getOriginalFilename());

        return new DownloadResult(resource, version.getOriginalFilename(), version.getContentType());
    }

    public DownloadResult previewVersion(Long planId, Long versionId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireViewAccess(user, plan);

        LessonPlanVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản giáo án."));

        if (!version.getPlan().getId().equals(planId)) {
            throw new ResourceNotFoundException("Phiên bản không thuộc giáo án này.");
        }

        if (version.getFilePurgedAt() != null) {
            throw new BusinessException("File đã được dọn để tiết kiệm dung lượng. Phiên bản này chỉ còn lưu hồ sơ thông tin.");
        }

        Resource resource = fileStorageService.loadFileAsResource(version.getStoredFilename());

        auditService.log("PLAN_PREVIEW", "LESSON_PLAN", plan.getId(),
            "Xem trước phiên bản " + version.getVersionNo() + ": " + version.getOriginalFilename());

        return new DownloadResult(resource, version.getOriginalFilename(), version.getContentType());
    }

    /**
     * Quy tắc 4.7: Job dọn file của version cũ hằng ngày lúc 02:00
     * - Giữ 3 version mới nhất và mọi version đã có đánh giá.
     * - Các version còn lại xoá file vật lý, giữ dòng DB và đánh dấu file_purged_at.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void purgeOldVersions() {
        log.info("Starting daily lesson plan version purge job...");
        List<LessonPlan> allPlans = lessonPlanRepository.findAll();
        int purgedCount = 0;

        for (LessonPlan plan : allPlans) {
            List<LessonPlanVersion> purgeable = versionRepository.findPurgeableVersions(
                plan.getId(), appProperties.getKeepVersions());

            for (LessonPlanVersion v : purgeable) {
                fileStorageService.deletePhysicalFile(v.getStoredFilename());
                v.setFilePurgedAt(Instant.now());
                versionRepository.save(v);
                purgedCount++;

                auditService.log("FILE_PURGED", "LESSON_PLAN_VERSION", v.getId(),
                    "Dọn file vật lý phiên bản " + v.getVersionNo() + " giáo án " + plan.getId() + " để tiết kiệm dung lượng");
            }
        }
        log.info("Purge job finished. Cleaned {} physical files.", purgedCount);
    }

    public List<LessonPlanVersion> getVersions(Long planId) {
        return versionRepository.findByPlanIdOrderByVersionNoDesc(planId);
    }

    public record DownloadResult(Resource resource, String originalFilename, String contentType) {}
}
