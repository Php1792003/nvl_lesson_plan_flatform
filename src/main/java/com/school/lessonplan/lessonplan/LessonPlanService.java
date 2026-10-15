package com.school.lessonplan.lessonplan;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class LessonPlanService {

    private static final Logger log = LoggerFactory.getLogger(LessonPlanService.class);

    /**
     * Danh sách năm học bắt đầu từ 2026-2027 và 5 năm học sau:
     * 2026-2027, 2027-2028, 2028-2029, 2029-2030, 2030-2031, 2031-2032
     */
    public static final List<String> DEFAULT_SCHOOL_YEARS = List.of(
            "2026-2027",
            "2027-2028",
            "2028-2029",
            "2029-2030",
            "2030-2031",
            "2031-2032"
    );

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

    public Page<LessonPlan> getFavoriteAccessiblePlans(CurrentUser user, Pageable pageable) {
        boolean isAdmin = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return lessonPlanRepository.findByFavoriteTrue(pageable);
        }
        return lessonPlanRepository.findFavoriteAccessiblePlans(user.getId(), pageable);
    }

    public List<String> getAvailableSchoolYears() {
        return DEFAULT_SCHOOL_YEARS;
    }

    public List<String> getSchoolYearsForPlan(String currentPlanSchoolYear) {
        Set<String> years = new LinkedHashSet<>(DEFAULT_SCHOOL_YEARS);
        if (currentPlanSchoolYear != null && !currentPlanSchoolYear.trim().isEmpty()) {
            years.add(currentPlanSchoolYear.trim());
        }
        return new ArrayList<>(years);
    }

    public List<String> getDistinctSchoolYears() {
        Set<String> years = new LinkedHashSet<>(DEFAULT_SCHOOL_YEARS);
        years.addAll(lessonPlanRepository.findDistinctSchoolYears());
        return new ArrayList<>(years);
    }

    public Page<LessonPlan> searchAllForAdmin(String search, Long departmentId, String schoolYear,
            Integer semester, Boolean favorite, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanSchoolYear = (schoolYear != null && !schoolYear.trim().isEmpty()) ? schoolYear.trim() : null;
        return lessonPlanRepository.searchAll(cleanSearch, departmentId, cleanSchoolYear, semester, favorite, pageable);
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
            Integer weekNo, Integer startPeriod, Integer endPeriod,
            String description, MultipartFile file) {
        User owner = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng."));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));

        // Lưu file vật lý
        FileStorageService.StoredFileResult stored = fileStorageService.storeFile(file);

        // Chuẩn hoá tiết học
        if (startPeriod != null && endPeriod == null) {
            endPeriod = startPeriod;
        } else if (startPeriod == null && endPeriod != null) {
            startPeriod = endPeriod;
        }
        if (startPeriod != null && endPeriod != null && endPeriod < startPeriod) {
            throw new BusinessException("Tiết kết thúc phải lớn hơn hoặc bằng tiết bắt đầu.");
        }

        String periodRange = null;
        if (startPeriod != null) {
            periodRange = startPeriod.equals(endPeriod) ? startPeriod.toString() : (startPeriod + "-" + endPeriod);
        }

        // Tạo LessonPlan
        LessonPlan plan = new LessonPlan();
        plan.setOwner(owner);
        plan.setTitle(title.trim());
        plan.setDepartment(department);
        plan.setGrade(grade.trim());
        plan.setSchoolYear(schoolYear.trim());
        plan.setSemester(semester);
        plan.setWeekNo(weekNo);
        plan.setStartPeriod(startPeriod);
        plan.setEndPeriod(endPeriod);
        plan.setPeriodRange(periodRange);
        plan.setPeriodNo(startPeriod);
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

        // Lên lịch xoá file vật lý cho các version cũ chưa lên lịch (sau N ngày)
        scheduleOldVersionsForDeletion(plan);

        auditService.log("PLAN_NEW_VERSION", "LESSON_PLAN", plan.getId(),
                "Tải lên phiên bản " + newVersionNo + " cho giáo án: " + plan.getTitle());

        return version;
    }

    /**
     * Lên lịch xoá tự động cho các version cũ (không phải version hiện tại)
     * chưa có scheduled_delete_at và chưa bị purge.
     * Thời gian xoá = now + app.version-delete-days (mặc định 30 ngày).
     */
    private void scheduleOldVersionsForDeletion(LessonPlan plan) {
        int deleteDays = appProperties.getVersionDeleteDays();
        Instant deleteAt = Instant.now().plus(Duration.ofDays(deleteDays));

        List<LessonPlanVersion> oldVersions = versionRepository.findOldVersionsNotScheduled(
                plan.getId(), plan.getCurrentVersionNo());

        for (LessonPlanVersion v : oldVersions) {
            v.setScheduledDeleteAt(deleteAt);
            versionRepository.save(v);

            log.info("Scheduled version {} (v{}) of plan {} for deletion at {}",
                    v.getId(), v.getVersionNo(), plan.getId(), deleteAt);
        }
    }

    /**
     * Xoá thủ công file vật lý của một version cũ.
     * Chỉ cho phép xoá version KHÔNG PHẢI version hiện tại.
     * Version đã có đánh giá KHÔNG được xoá.
     * Xoá hẳn file vật lý + đánh dấu file_purged_at.
     */
    @Transactional
    public void deleteOldVersion(Long planId, Long versionId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireManageAccess(user, plan);

        LessonPlanVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản giáo án."));

        if (!version.getPlan().getId().equals(planId)) {
            throw new ResourceNotFoundException("Phiên bản không thuộc giáo án này.");
        }

        // Không cho xoá version hiện tại
        if (version.getVersionNo() == plan.getCurrentVersionNo()) {
            throw new BusinessException("Không thể xoá phiên bản hiện tại. Chỉ có thể xoá các phiên bản cũ.");
        }

        // Không cho xoá nếu đã bị purge rồi
        if (version.isPurged()) {
            throw new BusinessException("Phiên bản này đã được dọn trước đó.");
        }

        // Không cho xoá nếu đã có đánh giá
        if (evaluationRepository.existsByVersionId(versionId)) {
            throw new BusinessException("Phiên bản đã có nhận xét/đánh giá từ Ban Giám Hiệu. Không thể xoá file.");
        }

        // Xoá file vật lý khỏi hệ thống
        fileStorageService.deletePhysicalFile(version.getStoredFilename());
        version.setFilePurgedAt(Instant.now());
        version.setScheduledDeleteAt(null); // Huỷ lịch tự động vì đã xoá thủ công
        versionRepository.save(version);

        auditService.log("VERSION_MANUAL_DELETE", "LESSON_PLAN_VERSION", version.getId(),
                "Xoá thủ công file vật lý phiên bản v" + version.getVersionNo() + " giáo án " + plan.getTitle());

        log.info("User {} manually deleted file of version {} (v{}) of plan {}",
                user.getId(), version.getId(), version.getVersionNo(), plan.getId());
    }

    @Transactional
    public LessonPlan updateMetadata(Long planId, CurrentUser user, String title,
            Long departmentId, String grade, String schoolYear,
            Integer semester, Integer weekNo, Integer startPeriod, Integer endPeriod, String description) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireManageAccess(user, plan);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("Bộ môn không tồn tại."));

        // Chuẩn hoá tiết học
        if (startPeriod != null && endPeriod == null) {
            endPeriod = startPeriod;
        } else if (startPeriod == null && endPeriod != null) {
            startPeriod = endPeriod;
        }
        if (startPeriod != null && endPeriod != null && endPeriod < startPeriod) {
            throw new FieldValidationException("endPeriod", "invalid", "Đến tiết phải lớn hơn hoặc bằng Từ tiết.");
        }

        String periodRange = null;
        if (startPeriod != null) {
            periodRange = startPeriod.equals(endPeriod) ? startPeriod.toString() : (startPeriod + "-" + endPeriod);
        }

        plan.setTitle(title.trim());
        plan.setDepartment(department);
        plan.setGrade(grade.trim());
        plan.setSchoolYear(schoolYear.trim());
        plan.setSemester(semester);
        plan.setWeekNo(weekNo);
        plan.setStartPeriod(startPeriod);
        plan.setEndPeriod(endPeriod);
        plan.setPeriodRange(periodRange);
        plan.setPeriodNo(startPeriod);
        plan.setDescription(description != null ? description.trim() : null);

        plan = lessonPlanRepository.save(plan);
        auditService.log("PLAN_UPDATE", "LESSON_PLAN", plan.getId(), "Cập nhật thông tin giáo án: " + plan.getTitle());
        return plan;
    }

    @Transactional
    public LessonPlan updateMetadata(Long planId, CurrentUser user, String title,
            Long departmentId, String grade, String schoolYear,
            Integer semester, Integer weekNo, String description) {
        return updateMetadata(planId, user, title, departmentId, grade, schoolYear, semester, weekNo, null, null, description);
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

        // Kiểm tra giáo án yêu thích
        if (plan.isFavorite()) {
            throw new BusinessException(
                    "Giáo án đang được đánh dấu Yêu thích (được bảo vệ khỏi các đợt dọn dẹp học kỳ của BGH). Vui lòng bỏ đánh dấu yêu thích trước nếu Thầy/Cô thực sự muốn xoá.");
        }

        // Kiểm tra đánh giá
        if (evaluationRepository.existsByPlanId(planId)) {
            throw new BusinessException(
                    "Giáo án đã có nhận xét/đánh giá từ Ban Giám Hiệu, không thể xoá. Thầy/Cô chỉ có thể tải lên phiên bản mới.");
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
            throw new BusinessException(
                    "File đã được dọn để tiết kiệm dung lượng. Phiên bản này chỉ còn lưu hồ sơ thông tin.");
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
            throw new BusinessException(
                    "File đã được dọn để tiết kiệm dung lượng. Phiên bản này chỉ còn lưu hồ sơ thông tin.");
        }

        Resource resource = fileStorageService.loadFileAsResource(version.getStoredFilename());

        auditService.log("PLAN_PREVIEW", "LESSON_PLAN", plan.getId(),
                "Xem trước phiên bản " + version.getVersionNo() + ": " + version.getOriginalFilename());

        return new DownloadResult(resource, version.getOriginalFilename(), version.getContentType());
    }

    /**
     * Quy tắc 4.7: Job dọn file của version cũ hằng ngày lúc 02:00
     * - Giữ 3 version mới nhất và mọi version đã có đánh giá.
     * - Các version còn lại xoá file vật lý, giữ dòng DB và đánh dấu
     * file_purged_at.
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
                        "Dọn file vật lý phiên bản " + v.getVersionNo() + " giáo án " + plan.getId()
                                + " để tiết kiệm dung lượng");
            }
        }
        log.info("Purge job finished. Cleaned {} physical files.", purgedCount);
    }

    /**
     * Job xoá tự động file vật lý của version cũ đã đến hạn scheduled_delete_at.
     * Chạy hàng ngày lúc 03:00.
     * - Xoá file vật lý khỏi đĩa.
     * - Đánh dấu file_purged_at.
     * - Bỏ qua version có đánh giá từ BGH.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteScheduledVersions() {
        log.info("Starting scheduled version auto-delete job...");
        List<LessonPlanVersion> dueVersions = versionRepository.findVersionsDueForDeletion(Instant.now());
        int deletedCount = 0;

        for (LessonPlanVersion v : dueVersions) {
            // Bỏ qua version có đánh giá
            if (evaluationRepository.existsByVersionId(v.getId())) {
                log.info("Skipping version {} (v{}) — has evaluations, will not auto-delete",
                        v.getId(), v.getVersionNo());
                v.setScheduledDeleteAt(null); // Huỷ lịch, giữ lại version
                versionRepository.save(v);
                continue;
            }

            fileStorageService.deletePhysicalFile(v.getStoredFilename());
            v.setFilePurgedAt(Instant.now());
            v.setScheduledDeleteAt(null);
            versionRepository.save(v);
            deletedCount++;

            auditService.log("VERSION_AUTO_DELETE", "LESSON_PLAN_VERSION", v.getId(),
                    "Tự động xoá file vật lý phiên bản v" + v.getVersionNo()
                            + " giáo án " + v.getPlan().getId()
                            + " sau " + appProperties.getVersionDeleteDays() + " ngày");
        }
        log.info("Auto-delete job finished. Deleted {} physical files.", deletedCount);
    }

    /**
     * Bật/tắt trạng thái yêu thích của giáo án.
     * Giáo án được yêu thích sẽ được bảo vệ tuyệt đối khi Ban Giám Hiệu dọn dẹp
     * cuối kỳ.
     */
    @Transactional
    public boolean toggleFavorite(Long planId, CurrentUser user) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án."));

        accessService.requireViewAccess(user, plan);

        boolean newFavorite = !plan.isFavorite();
        plan.setFavorite(newFavorite);
        plan.setFavoritedAt(newFavorite ? Instant.now() : null);
        lessonPlanRepository.save(plan);

        String action = newFavorite ? "PLAN_FAVORITED" : "PLAN_UNFAVORITED";
        String detail = (newFavorite ? "Đánh dấu yêu thích giáo án (bảo vệ khỏi dọn dẹp học kỳ): "
                : "Bỏ đánh dấu yêu thích giáo án: ") + plan.getTitle();
        auditService.log(action, "LESSON_PLAN", plan.getId(), detail);

        log.info("User {} toggled favorite for plan {}: {}", user.getId(), plan.getId(), newFavorite);
        return newFavorite;
    }

    public record SemesterCleanupResult(int deletedCount, int protectedFavoriteCount, int protectedEvaluatedCount) {
    }

    /**
     * Chức năng Ban Giám Hiệu dọn dẹp giáo án sau mỗi học kỳ.
     * Quy tắc an toàn tối thượng:
     * - BẢO VỆ TUYỆT ĐỐI các giáo án được đánh dấu Yêu thích (is_favorite = true)
     * -> KHÔNG XOÁ.
     * - Giữ lại các giáo án đã có nhận xét/đánh giá từ BGH (theo Rule 4.5b).
     * - Chỉ xoá các giáo án thông thường không được yêu thích và chưa có đánh giá
     * (xoá file vật lý và bản ghi).
     */
    @Transactional
    public SemesterCleanupResult cleanupSemesterPlans(String schoolYear, int semester, CurrentUser user) {
        boolean isBgh = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("PLAN_VIEW_ALL") || a.equals("ROLE_ADMIN"));
        if (!isBgh) {
            throw new BusinessException("Chỉ Ban Giám Hiệu mới có quyền dọn dẹp giáo án theo học kỳ.");
        }

        List<LessonPlan> plans = lessonPlanRepository.findBySchoolYearAndSemester(schoolYear, semester);
        int deletedCount = 0;
        int protectedFavoriteCount = 0;
        int protectedEvaluatedCount = 0;

        for (LessonPlan plan : plans) {
            // 1. Giáo án YÊU THÍCH -> BẢO VỆ TUYỆT ĐỐI, BỎ QUA KHÔNG XOÁ!
            if (plan.isFavorite()) {
                protectedFavoriteCount++;
                continue;
            }

            // 2. Giáo án đã có đánh giá từ BGH -> BẢO VỆ GIỮ LẠI (theo Rule 4.5b)
            if (evaluationRepository.existsByPlanId(plan.getId())) {
                protectedEvaluatedCount++;
                continue;
            }

            // 3. Giáo án thông thường -> Xoá file vật lý của tất cả versions
            List<LessonPlanVersion> versions = versionRepository.findAllByPlanId(plan.getId());
            for (LessonPlanVersion v : versions) {
                fileStorageService.deletePhysicalFile(v.getStoredFilename());
            }

            // Xoá giáo án khỏi DB (cascade xoá versions và shares)
            lessonPlanRepository.delete(plan);
            deletedCount++;
        }

        auditService.log("SEMESTER_CLEANUP", "LESSON_PLAN", null,
                String.format(
                        "BGH dọn dẹp học kỳ %d (%s): Đã xoá %d giáo án, bảo vệ an toàn %d giáo án yêu thích, giữ lại %d giáo án có nhận xét.",
                        semester, schoolYear, deletedCount, protectedFavoriteCount, protectedEvaluatedCount));

        log.info("Semester cleanup completed for {} HK{}: deleted={}, protectedFavorites={}, protectedEvaluated={}",
                schoolYear, semester, deletedCount, protectedFavoriteCount, protectedEvaluatedCount);

        return new SemesterCleanupResult(deletedCount, protectedFavoriteCount, protectedEvaluatedCount);
    }

    public List<LessonPlanVersion> getVersions(Long planId) {
        return versionRepository.findByPlanIdOrderByVersionNoDesc(planId);
    }

    public record DownloadResult(Resource resource, String originalFilename, String contentType) {
    }
}
