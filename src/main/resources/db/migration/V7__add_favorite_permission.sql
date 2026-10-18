-- ============================================================
-- V7__add_favorite_permission.sql
-- Thêm quyền PLAN_FAVORITE_VIEW (Xem danh sách giáo án yêu thích)
-- Mặc định gán cho vai trò ADMIN (Ban Giám Hiệu). Giáo viên chỉ xem được khi được cấp quyền.
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PLAN_FAVORITE_VIEW')
BEGIN
    INSERT INTO permissions (code) VALUES ('PLAN_FAVORITE_VIEW');
END
GO

-- Gán quyền PLAN_FAVORITE_VIEW cho vai trò ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN' 
  AND p.code = 'PLAN_FAVORITE_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
GO
