-- ============================================================
-- V12__create_nq52_evaluations_table.sql
-- Triển khai Đánh giá, xếp loại viên chức theo Nghị quyết 52 (Mẫu 03)
-- ============================================================

-- 1. Thêm quyền NQ52_REVIEW (Đánh giá, xếp loại NQ52 cấp Tổ / BGH)
IF NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'NQ52_REVIEW')
BEGIN
    INSERT INTO permissions (code) VALUES ('NQ52_REVIEW');
END
GO

-- 2. Gán quyền NQ52_REVIEW cho vai trò ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN' 
  AND p.code = 'NQ52_REVIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
GO

-- 3. Thêm vai trò DEPT_LEADER (Tổ trưởng chuyên môn) nếu chưa có
IF NOT EXISTS (SELECT 1 FROM roles WHERE code = 'DEPT_LEADER')
BEGIN
    INSERT INTO roles (code, name) VALUES ('DEPT_LEADER', N'Tổ trưởng chuyên môn');
END
GO

-- 4. Gán quyền NQ52_REVIEW cho DEPT_LEADER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'DEPT_LEADER' 
  AND p.code = 'NQ52_REVIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
GO

-- 5. Tạo bảng nq52_evaluations
CREATE TABLE nq52_evaluations (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                 BIGINT               NOT NULL,
    department_id           BIGINT               NOT NULL,
    evaluation_month        INT                  NOT NULL,
    evaluation_year         INT                  NOT NULL,
    status                  NVARCHAR(30)         NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, REVIEWED
    
    -- Thông tin hiển thị tại thời điểm đánh giá
    teacher_name_snapshot   NVARCHAR(400)        NOT NULL,
    position_title          NVARCHAR(200)        NOT NULL DEFAULT N'Giáo viên',
    work_unit               NVARCHAR(300)        NOT NULL DEFAULT N'Trường THCS Nguyễn Văn Linh',
    department_name_snapshot NVARCHAR(200)       NOT NULL,

    -- Điểm cá nhân tự chấm
    self_score_a            DECIMAL(5, 2)        NOT NULL DEFAULT 0.00,
    self_score_b            DECIMAL(5, 2)        NOT NULL DEFAULT 0.00,
    self_score_c_plus       DECIMAL(5, 2)        NOT NULL DEFAULT 0.00,
    self_score_c_minus      DECIMAL(5, 2)        NOT NULL DEFAULT 0.00,
    self_total_score        DECIMAL(5, 2)        NOT NULL DEFAULT 0.00,
    self_classification     NVARCHAR(100)        NULL,
    strengths               NVARCHAR(2000)       NULL,
    weaknesses              NVARCHAR(2000)       NULL,
    
    -- Điểm tổ trưởng đánh giá
    reviewer_score_a        DECIMAL(5, 2)        NULL,
    reviewer_score_b        DECIMAL(5, 2)        NULL,
    reviewer_score_c_plus   DECIMAL(5, 2)        NULL,
    reviewer_score_c_minus  DECIMAL(5, 2)        NULL,
    reviewer_total_score    DECIMAL(5, 2)        NULL,
    reviewer_classification NVARCHAR(100)        NULL,
    reviewer_notes          NVARCHAR(2000)       NULL,
    reviewer_id             BIGINT               NULL,
    reviewer_name_snapshot  NVARCHAR(400)        NULL,
    
    -- Dữ liệu chi tiết từng tiêu chí dạng JSON
    criteria_data_json      NVARCHAR(MAX)        NULL,
    
    submitted_at            DATETIME2            NULL,
    reviewed_at             DATETIME2            NULL,
    created_at              DATETIME2            NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2            NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT uq_nq52_user_period UNIQUE (user_id, evaluation_month, evaluation_year),
    CONSTRAINT fk_nq52_user       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nq52_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_nq52_reviewer   FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE INDEX ix_nq52_user ON nq52_evaluations(user_id);
CREATE INDEX ix_nq52_dept_status ON nq52_evaluations(department_id, status);
CREATE INDEX ix_nq52_period ON nq52_evaluations(evaluation_year, evaluation_month);
GO

