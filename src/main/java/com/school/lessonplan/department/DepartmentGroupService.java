package com.school.lessonplan.department;

import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentGroupService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentGroupService.class);

    public enum SubjectGroup {
        TOAN_TIN("Tổ Toán - Tin"),
        KHTN("Tổ Khoa học tự nhiên (KHTN)"),
        VAN_GDCD("Tổ Ngữ văn - GDCD"),
        LSDL("Tổ Lịch sử & Địa lý"),
        TIENG_ANH("Tổ Tiếng Anh"),
        OTHER("Tổ chuyên môn");

        private final String displayName;

        SubjectGroup(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentGroupService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Chuẩn hoá chuỗi tiếng Việt (chữ thường, không dấu) để phân loại tổ chuyên môn
     */
    public String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replace("đ", "d")
                .trim();
    }

    /**
     * Phân loại bộ môn vào Tổ chuyên môn tương ứng theo quy định của nhà trường:
     * - Tổ Toán - Tin: Toán học, Tin học
     * - Tổ KHTN: KHTN, KHTN1, KHTN2, KHTN3 (và Vật lý, Hoá học, Sinh học)
     * - Tổ Văn - GDCD: Ngữ văn, Giáo dục công dân
     * - Tổ LSĐL: Lịch sử và Địa lý (và Giáo dục địa phương)
     * - Tổ Tiếng Anh: Tiếng Anh (Ngoại ngữ)
     */
    public SubjectGroup resolveGroup(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            return SubjectGroup.OTHER;
        }
        String norm = normalize(departmentName);

        // 1. Tổ Toán - Tin
        if (norm.contains("toan") || norm.contains("tin hoc") || norm.equals("tin")) {
            return SubjectGroup.TOAN_TIN;
        }

        // 2. Tổ Lịch sử & Địa lý (kiểm tra trước KHTN để tránh 'dia ly' dính chữ 'ly')
        if (norm.contains("lich su") || norm.contains("dia ly") || norm.contains("lsdl")
                || norm.contains("dia phuong") || norm.contains("giao duc dia phuong")) {
            return SubjectGroup.LSDL;
        }

        // 3. Tổ KHTN
        if (norm.contains("khtn") || norm.contains("khoa hoc tu nhien")
                || norm.contains("vat ly") || norm.contains("hoa hoc") || norm.contains("sinh hoc")
                || norm.contains("vat li") || norm.equals("ly") || norm.equals("hoa") || norm.equals("sinh")) {
            return SubjectGroup.KHTN;
        }

        // 4. Tổ Văn - GDCD
        if (norm.contains("ngu van") || norm.equals("van") || norm.contains("gdcd")
                || norm.contains("giao duc cong dan") || norm.contains("cong dan")) {
            return SubjectGroup.VAN_GDCD;
        }

        // 5. Tổ Tiếng Anh
        if (norm.contains("tieng anh") || norm.contains("ngoai ngu") || norm.equals("anh")) {
            return SubjectGroup.TIENG_ANH;
        }

        return SubjectGroup.OTHER;
    }

    /**
     * Kiểm tra user hiện tại có phải là Tổ trưởng chuyên môn hay không.
     * Ban Giám Hiệu (ROLE_ADMIN) không bị giới hạn theo tổ.
     */
    public boolean isDepartmentHead(CurrentUser currentUser) {
        if (currentUser == null) return false;
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return false;

        boolean hasDeptHeadAuthority = currentUser.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority().toUpperCase();
                    return auth.equals("PLAN_VIEW_ALL")
                            || auth.equals("PLAN_VIEW_DEPARTMENT")
                            || auth.contains("TO_TRUONG")
                            || auth.contains("TTCM")
                            || auth.contains("DEPARTMENT_HEAD")
                            || auth.contains("DEPT_LEADER");
                });

        log.debug("[DeptGroup] isDepartmentHead check for user={}, authorities={}, hasDeptHeadAuthority={}",
                currentUser.getUsername(), currentUser.getAuthorities(), hasDeptHeadAuthority);

        if (hasDeptHeadAuthority) return true;

        return userRepository.findByIdWithRolesAndPermissions(currentUser.getId())
                .map(this::isDepartmentHead)
                .orElse(false);
    }

    public boolean isDepartmentHead(User user) {
        if (user == null) return false;
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
        if (isAdmin) return false;

        return user.getRoles().stream()
                .anyMatch(r -> {
                    String code = r.getCode() != null ? r.getCode().toUpperCase() : "";
                    String name = r.getName() != null ? r.getName().toLowerCase() : "";
                    boolean codeMatch = code.contains("TO_TRUONG") || code.contains("TTCM")
                            || code.contains("DEPARTMENT_HEAD") || code.contains("DEPT_LEADER")
                            || code.contains("HEAD") || code.contains("LEADER");
                    boolean nameMatch = name.contains("tổ trưởng") || name.contains("to truong") || name.contains("chuyên môn");
                    boolean permMatch = r.getPermissions().stream()
                            .anyMatch(p -> "PLAN_VIEW_ALL".equalsIgnoreCase(p.getCode())
                                    || "PLAN_VIEW_DEPARTMENT".equalsIgnoreCase(p.getCode()));
                    return codeMatch || nameMatch || permMatch;
                });
    }

    /**
     * Lấy tên môn học đã đăng ký của user
     */
    @Transactional(readOnly = true)
    public String getUserDepartmentName(CurrentUser currentUser) {
        if (currentUser == null) return null;
        if (currentUser.getDepartmentName() != null && !currentUser.getDepartmentName().isEmpty()) {
            return currentUser.getDepartmentName();
        }
        return userRepository.findByIdWithRolesAndPermissions(currentUser.getId())
                .map(u -> u.getDepartment() != null ? u.getDepartment().getName() : null)
                .orElse(null);
    }

    /**
     * Lấy tên hiển thị của Tổ chuyên môn mà user phụ trách
     */
    @Transactional(readOnly = true)
    public String getDepartmentGroupName(CurrentUser currentUser) {
        if (currentUser == null) return "";
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return "Ban Giám Hiệu";
        }

        String deptName = getUserDepartmentName(currentUser);
        log.debug("[DeptGroup] getDepartmentGroupName: user={}, deptName={}", currentUser.getUsername(), deptName);

        if (deptName == null || deptName.isEmpty()) {
            return "Tổ chuyên môn";
        }

        SubjectGroup group = resolveGroup(deptName);
        if (group != SubjectGroup.OTHER) {
            return group.getDisplayName();
        }
        return "Tổ " + deptName;
    }

    /**
     * Lấy danh sách các môn học thuộc Tổ chuyên môn của user.
     * - Nếu là Ban Giám Hiệu (ROLE_ADMIN): trả về toàn bộ môn trong trường.
     * - Nếu là Tổ trưởng: trả về tất cả các môn trong tổ hợp của tổ đó.
     */
    @Transactional(readOnly = true)
    public List<Department> getAccessibleDepartments(CurrentUser currentUser) {
        List<Department> allDepts = departmentRepository.findAll();
        if (currentUser == null) return Collections.emptyList();

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return allDepts;
        }

        String deptName = getUserDepartmentName(currentUser);
        log.debug("[DeptGroup] getAccessibleDepartments: user={}, deptName={}, allDepts={}",
                currentUser.getUsername(), deptName,
                allDepts.stream().map(d -> d.getId() + ":" + d.getName()).collect(Collectors.joining(", ")));

        if (deptName == null || deptName.isEmpty()) {
            return Collections.emptyList();
        }

        SubjectGroup group = resolveGroup(deptName);
        log.debug("[DeptGroup] resolvedGroup={}", group);

        if (group == SubjectGroup.OTHER) {
            return allDepts.stream()
                    .filter(d -> d.getName().equalsIgnoreCase(deptName))
                    .collect(Collectors.toList());
        }

        List<Department> filtered = allDepts.stream()
                .filter(d -> resolveGroup(d.getName()) == group)
                .collect(Collectors.toList());
        log.debug("[DeptGroup] filteredDepts={}", filtered.stream().map(d -> d.getId() + ":" + d.getName()).collect(Collectors.joining(", ")));
        return filtered;
    }

    /**
     * Lấy danh sách ID các môn học mà user có quyền xem.
     * - Ban Giám Hiệu: trả về null (không giới hạn ID nào).
     * - Tổ trưởng chuyên môn: trả về danh sách ID các môn thuộc tổ hợp.
     * - Giáo viên thông thường: trả về danh sách rỗng (không xem toàn bộ).
     */
    @Transactional(readOnly = true)
    public List<Long> getAccessibleDepartmentIds(CurrentUser currentUser) {
        if (currentUser == null) return Collections.emptyList();

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return null; // Không hạn chế
        }

        if (!isDepartmentHead(currentUser)) {
            return Collections.emptyList();
        }

        List<Long> ids = getAccessibleDepartments(currentUser).stream()
                .map(Department::getId)
                .collect(Collectors.toList());
        log.debug("[DeptGroup] getAccessibleDepartmentIds: user={}, ids={}", currentUser.getUsername(), ids);
        return ids;
    }

    @Transactional(readOnly = true)
    public List<Long> getAccessibleDepartmentIds(User user) {
        if (user == null) return Collections.emptyList();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
        if (isAdmin) {
            return null;
        }

        if (!isDepartmentHead(user)) {
            return Collections.emptyList();
        }

        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;
        if (deptName == null || deptName.isEmpty()) {
            return Collections.emptyList();
        }

        SubjectGroup group = resolveGroup(deptName);
        List<Department> allDepts = departmentRepository.findAll();

        if (group == SubjectGroup.OTHER) {
            return allDepts.stream()
                    .filter(d -> d.getName().equalsIgnoreCase(deptName))
                    .map(Department::getId)
                    .collect(Collectors.toList());
        }

        return allDepts.stream()
                .filter(d -> resolveGroup(d.getName()) == group)
                .map(Department::getId)
                .collect(Collectors.toList());
    }
}
