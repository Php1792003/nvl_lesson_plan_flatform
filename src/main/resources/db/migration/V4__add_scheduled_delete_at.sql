-- ============================================================
-- V4__add_scheduled_delete_at.sql
-- Thêm cột scheduled_delete_at để lên lịch xoá version cũ sau 30 ngày
-- ============================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('lesson_plan_versions') 
      AND name = 'scheduled_delete_at'
)
BEGIN
    ALTER TABLE lesson_plan_versions
        ADD scheduled_delete_at DATETIME2 NULL;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE object_id = OBJECT_ID('lesson_plan_versions') 
      AND name = 'ix_lpv_scheduled_delete'
)
BEGIN
    CREATE INDEX ix_lpv_scheduled_delete
        ON lesson_plan_versions(scheduled_delete_at)
        WHERE scheduled_delete_at IS NOT NULL;
END
GO
