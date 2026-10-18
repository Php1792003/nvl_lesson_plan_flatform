-- ============================================================
-- V8__update_campus_name_to_diem_truong_chinh.sql
-- Đổi 'Phân hiệu 1' thành 'Điểm trường chính'
-- ============================================================

UPDATE campuses
SET name = N'Điểm trường chính'
WHERE name = N'Phân hiệu 1' OR name = N'Nguyễn Văn Linh';
