-- ============================================================
-- V26__add_to_van_phong_department.sql
-- Thêm Tổ Văn Phòng vào danh mục phòng ban / tổ chuyên môn
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM departments WHERE name = N'Tổ Văn Phòng')
BEGIN
    INSERT INTO departments (name) VALUES (N'Tổ Văn Phòng');
END;
