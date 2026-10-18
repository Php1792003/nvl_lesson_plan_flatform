package com.school.lessonplan.role;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    // Danh sách các hằng số quyền hạn cụ thể (Constants)
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_LOCK = "USER_LOCK";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String PLAN_VIEW_ALL = "PLAN_VIEW_ALL";
    public static final String EVALUATION_CREATE = "EVALUATION_CREATE";
    public static final String DEPARTMENT_MANAGE = "DEPARTMENT_MANAGE";
    public static final String CAMPUS_MANAGE = "CAMPUS_MANAGE";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String PLAN_FAVORITE_VIEW = "PLAN_FAVORITE_VIEW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    public Permission() {
    }

    public Permission(String code) {
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Tên mô tả quyền hạn cụ thể bằng tiếng Việt dễ hiểu
     */
    public String getDescription() {
        if (code == null)
            return "";
        return switch (code) {
            case USER_UPDATE -> "Sửa thông tin giáo viên";
            case USER_VIEW -> "Xem danh sách giáo viên";
            case USER_CREATE -> "Thêm giáo viên mới";
            case USER_DELETE -> "Xoá tài khoản giáo viên";
            case USER_LOCK -> "Khoá / Mở khoá tài khoản";
            case ROLE_MANAGE -> "Quản lý vai trò & quyền hạn";
            case PLAN_VIEW_ALL -> "Xem toàn bộ giáo án trường";
            case EVALUATION_CREATE -> "Tạo nhận xét / đánh giá";
            case DEPARTMENT_MANAGE -> "Quản lý Môn học";
            case CAMPUS_MANAGE -> "Quản lý cơ sở / phân hiệu";
            case AUDIT_VIEW -> "Xem nhật ký hệ thống";
            case PLAN_FAVORITE_VIEW -> "Xem danh sách giáo án yêu thích";
            default -> code;
        };
    }

    public String getDisplayName() {
        return getDescription();
    }
}
