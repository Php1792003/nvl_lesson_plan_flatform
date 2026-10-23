-- ============================================================
-- V11__create_pending_registrations_table.sql
-- Lưu tạm thông tin đăng ký và mã OTP trước khi kích hoạt
-- Chỉ tạo bản ghi trong bảng users SAU KHI đã xác thực OTP thành công.
-- ============================================================

CREATE TABLE pending_registrations (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    email           NVARCHAR(150)   NOT NULL,
    last_name       NVARCHAR(50)    NOT NULL,
    given_name      NVARCHAR(80)    NOT NULL,
    password_hash   NVARCHAR(255)   NOT NULL,
    department_id   BIGINT          NOT NULL,
    campus_id       BIGINT          NOT NULL,
    code_hash       NVARCHAR(500)   NOT NULL,
    expires_at      DATETIME2       NOT NULL,
    attempts        INT             NOT NULL DEFAULT 0,
    last_sent_at    DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT uq_pending_registrations_email UNIQUE (email),
    CONSTRAINT fk_pending_reg_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_pending_reg_camp FOREIGN KEY (campus_id) REFERENCES campuses(id)
);

CREATE INDEX ix_pending_reg_email ON pending_registrations(email);
CREATE INDEX ix_pending_reg_expires ON pending_registrations(expires_at);

-- Dọn dẹp các tài khoản chưa xác thực email (PENDING_EMAIL) tồn đọng trước đây
-- để giải phóng email cho giáo viên có thể đăng ký lại bình thường
DELETE FROM email_verification_codes WHERE user_id IN (SELECT id FROM users WHERE status = 'PENDING_EMAIL');
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE status = 'PENDING_EMAIL');
DELETE FROM users WHERE status = 'PENDING_EMAIL';
