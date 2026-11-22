-- ============================================================
-- V15__seed_department_head_role.sql
-- Tự động tạo sẵn vai trò Tổ trưởng chuyên môn và gán quyền chuẩn
-- ============================================================

-- 1. Nếu chưa có vai trò Tổ trưởng chuyên môn, tạo mới
IF NOT EXISTS (
    SELECT 1 FROM roles 
    WHERE code IN ('TO_TRUONG', 'TO_TRUONG_CHUYEN_MON', 'TTCM')
       OR LOWER(name) LIKE N'%tổ trưởng%'
)
BEGIN
    INSERT INTO roles (code, name) VALUES ('TO_TRUONG', N'Tổ trưởng chuyên môn');
END
GO

-- 2. Gán các quyền chuẩn (PLAN_VIEW_ALL, EVALUATION_CREATE, PLAN_FAVORITE_VIEW) cho vai trò Tổ trưởng chuyên môn
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE (r.code IN ('TO_TRUONG', 'TO_TRUONG_CHUYEN_MON', 'TTCM') OR LOWER(r.name) LIKE N'%tổ trưởng%')
  AND p.code IN ('PLAN_VIEW_ALL', 'EVALUATION_CREATE', 'PLAN_FAVORITE_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
GO

