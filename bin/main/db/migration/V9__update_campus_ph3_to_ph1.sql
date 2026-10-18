-- ============================================================
-- V9__update_campus_ph3_to_ph1.sql
-- Đổi 'Phân hiệu 3' thành 'Phân hiệu 1'
-- Danh sách phân hiệu gồm:
-- 1. Điểm trường chính (mặc định)
-- 2. Phân hiệu 1
-- 3. Phân hiệu 2
-- ============================================================

-- 1. Nếu cả 'Phân hiệu 3' và 'Phân hiệu 1' đều tồn tại:
-- Chuyển user từ Phân hiệu 3 sang Phân hiệu 1, sau đó xóa Phân hiệu 3
IF EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 3') AND EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 1')
BEGIN
    DECLARE @Ph1Id BIGINT;
    DECLARE @Ph3Id BIGINT;
    SELECT @Ph1Id = id FROM campuses WHERE name = N'Phân hiệu 1';
    SELECT @Ph3Id = id FROM campuses WHERE name = N'Phân hiệu 3';

    UPDATE users SET campus_id = @Ph1Id WHERE campus_id = @Ph3Id;
    DELETE FROM campuses WHERE id = @Ph3Id;
END
-- 2. Nếu 'Phân hiệu 3' tồn tại và 'Phân hiệu 1' chưa tồn tại:
-- Đổi tên 'Phân hiệu 3' thành 'Phân hiệu 1'
ELSE IF EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 3') AND NOT EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 1')
BEGIN
    UPDATE campuses
    SET name = N'Phân hiệu 1'
    WHERE name = N'Phân hiệu 3';
END
-- 3. Nếu 'Phân hiệu 1' chưa tồn tại:
ELSE IF NOT EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 1')
BEGIN
    INSERT INTO campuses (name, is_default) VALUES (N'Phân hiệu 1', 0);
END

-- Đảm bảo 'Điểm trường chính' và 'Phân hiệu 2' tồn tại đúng trạng thái
IF NOT EXISTS (SELECT 1 FROM campuses WHERE name = N'Điểm trường chính')
BEGIN
    INSERT INTO campuses (name, is_default) VALUES (N'Điểm trường chính', 1);
END

IF NOT EXISTS (SELECT 1 FROM campuses WHERE name = N'Phân hiệu 2')
BEGIN
    INSERT INTO campuses (name, is_default) VALUES (N'Phân hiệu 2', 0);
END

-- Đảm bảo chỉ 'Điểm trường chính' là mặc định
UPDATE campuses SET is_default = 1 WHERE name = N'Điểm trường chính';
UPDATE campuses SET is_default = 0 WHERE name IN (N'Phân hiệu 1', N'Phân hiệu 2');

-- Cập nhật snapshot trong lesson_plans nếu có giáo án lưu 'Phân hiệu 3'
UPDATE lesson_plans
SET owner_snapshot_campus = N'Phân hiệu 1'
WHERE owner_snapshot_campus = N'Phân hiệu 3';
