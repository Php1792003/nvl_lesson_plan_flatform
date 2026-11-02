-- ============================================================
-- V14__add_week_range_to_lesson_plans.sql
-- Hỗ trợ khoảng tuần học (Từ tuần - Đến tuần, ví dụ: Tuần 1 - 5)
-- ============================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('lesson_plans') 
      AND name = 'end_week_no'
)
BEGIN
    ALTER TABLE lesson_plans
        ADD end_week_no INT NULL,
            week_range  NVARCHAR(50) NULL;
END
GO

-- Đồng bộ dữ liệu cũ nếu đã có week_no
UPDATE lesson_plans
SET end_week_no = week_no,
    week_range  = CAST(week_no AS NVARCHAR(50))
WHERE week_no IS NOT NULL AND end_week_no IS NULL;
GO

