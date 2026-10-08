package com.school.lessonplan.role;

import com.school.lessonplan.audit.AuditService;
import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Role getById(Long id) {
        return roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò."));
    }

    @Transactional
    public Role createRole(String code, String name, List<Long> permissionIds) {
        String cleanCode = code.trim().toUpperCase();
        if (roleRepository.existsByCode(cleanCode)) {
            throw new FieldValidationException("code", "Duplicate", "Mã vai trò đã tồn tại.");
        }

        Role role = new Role();
        role.setCode(cleanCode);
        role.setName(name.trim());

        if (permissionIds != null && !permissionIds.isEmpty()) {
            role.setPermissions(new HashSet<>(permissionRepository.findAllById(permissionIds)));
        }

        role = roleRepository.save(role);
        auditService.log("ROLE_CREATE", "ROLE", role.getId(), "Tạo vai trò: " + role.getCode());
        return role;
    }

    @Transactional
    public Role updateRole(Long id, String name, List<Long> permissionIds) {
        Role role = getById(id);
        if ("ADMIN".equals(role.getCode()) || "TEACHER".equals(role.getCode())) {
            // Không đổi tên hệ thống của ADMIN/TEACHER nhưng có thể chỉnh permissions nếu cần
        }
        role.setName(name.trim());

        if (permissionIds != null) {
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(permissionIds));
            role.setPermissions(perms);
        }

        role = roleRepository.save(role);
        auditService.log("ROLE_UPDATE", "ROLE", role.getId(), "Cập nhật vai trò: " + role.getCode());
        return role;
    }
}
