-- ============================================================
-- V25__add_nq52_access_permission.sql
-- Thêm quyền NQ52_ACCESS (Sử dụng tính năng Nghị quyết 52)
-- Mặc định gán cho vai trò ADMIN (Ban Giám Hiệu). Giáo viên chỉ truy cập được khi được cấp quyền.
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'NQ52_ACCESS')
BEGIN
    INSERT INTO permissions (code) VALUES ('NQ52_ACCESS');
END
GO

-- Đảm bảo quyền NQ52_REVIEW tồn tại
IF NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'NQ52_REVIEW')
BEGIN
    INSERT INTO permissions (code) VALUES ('NQ52_REVIEW');
END
GO

-- Gán quyền NQ52_ACCESS và NQ52_REVIEW cho vai trò ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN' 
  AND p.code IN ('NQ52_ACCESS', 'NQ52_REVIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
GO

