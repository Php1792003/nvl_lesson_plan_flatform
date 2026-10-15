-- ============================================================
-- V6__add_period_range_to_lesson_plans.sql
-- Tách riêng thông tin Tuần học và Tiết học (PPCT)
-- Hỗ trợ giáo án 1 tiết, khoảng tiết (1-3, 1-10, ...)
-- ============================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('lesson_plans') 
      AND name = 'start_period'
)
BEGIN
    ALTER TABLE lesson_plans
        ADD start_period INT NULL,
            end_period   INT NULL,
            period_range NVARCHAR(50) NULL;
END
GO

-- Đồng bộ dữ liệu cũ nếu đã có period_no
UPDATE lesson_plans
SET start_period = period_no,
    end_period   = period_no,
    period_range = CAST(period_no AS NVARCHAR(50))
WHERE period_no IS NOT NULL AND start_period IS NULL;
GO
