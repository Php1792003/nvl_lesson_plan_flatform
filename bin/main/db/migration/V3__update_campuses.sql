-- ============================================================
-- V3__update_campuses.sql — Cập nhật danh sách phân hiệu
-- ============================================================

-- Đổi 'Nguyễn Văn Linh' thành 'Phân hiệu 1'
UPDATE campuses
SET name = N'Phân hiệu 1'
WHERE name = N'Nguyễn Văn Linh';

-- Thêm 'Phân hiệu 3' nếu chưa tồn tại
IF NOT EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 3')
BEGIN
    INSERT INTO campuses (name, is_default) VALUES (N'Phân hiệu 3', 0);
END
