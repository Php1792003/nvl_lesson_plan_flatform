-- ============================================================
-- V2__seed.sql — Dữ liệu seed ban đầu
-- ============================================================

-- Phân hiệu mặc định
INSERT INTO campuses (name, is_default) VALUES (N'Nguyễn Văn Linh', 1);
INSERT INTO campuses (name, is_default) VALUES (N'Phân hiệu 2', 0);

-- Bộ môn mẫu
INSERT INTO departments (name) VALUES (N'Toán học');
INSERT INTO departments (name) VALUES (N'Ngữ văn');
INSERT INTO departments (name) VALUES (N'Tiếng Anh');
INSERT INTO departments (name) VALUES (N'Vật lý');
INSERT INTO departments (name) VALUES (N'Hoá học');
INSERT INTO departments (name) VALUES (N'Sinh học');
INSERT INTO departments (name) VALUES (N'Lịch sử');
INSERT INTO departments (name) VALUES (N'Địa lý');
INSERT INTO departments (name) VALUES (N'Giáo dục công dân');
INSERT INTO departments (name) VALUES (N'Tin học');
INSERT INTO departments (name) VALUES (N'Công nghệ');
INSERT INTO departments (name) VALUES (N'Thể dục');
INSERT INTO departments (name) VALUES (N'Âm nhạc');
INSERT INTO departments (name) VALUES (N'Mỹ thuật');

-- Vai trò
INSERT INTO roles (code, name) VALUES ('ADMIN', N'Ban Giám Hiệu');
INSERT INTO roles (code, name) VALUES ('TEACHER', N'Giáo viên');

-- Quyền
INSERT INTO permissions (code) VALUES ('USER_VIEW');
INSERT INTO permissions (code) VALUES ('USER_CREATE');
INSERT INTO permissions (code) VALUES ('USER_UPDATE');
INSERT INTO permissions (code) VALUES ('USER_DELETE');
INSERT INTO permissions (code) VALUES ('USER_LOCK');
INSERT INTO permissions (code) VALUES ('ROLE_MANAGE');
INSERT INTO permissions (code) VALUES ('PLAN_VIEW_ALL');
INSERT INTO permissions (code) VALUES ('EVALUATION_CREATE');
INSERT INTO permissions (code) VALUES ('DEPARTMENT_MANAGE');
INSERT INTO permissions (code) VALUES ('CAMPUS_MANAGE');
INSERT INTO permissions (code) VALUES ('AUDIT_VIEW');

-- Gán toàn bộ quyền cho ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

-- Tài khoản admin đầu tiên sẽ được seed bằng ApplicationRunner trong Java
-- vì cần hash mật khẩu bằng BCrypt (không thể làm trong SQL thuần)
