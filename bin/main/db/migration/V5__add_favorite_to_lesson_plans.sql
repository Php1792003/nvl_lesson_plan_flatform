-- ============================================================
-- V5__add_favorite_to_lesson_plans.sql
-- Thêm cột is_favorite và favorited_at để đánh dấu giáo án yêu thích,
-- bảo vệ giáo án không bị Ban Giám Hiệu xoá sau mỗi học kỳ
-- ============================================================

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('lesson_plans') 
      AND name = 'is_favorite'
)
BEGIN
    ALTER TABLE lesson_plans
        ADD is_favorite BIT NOT NULL CONSTRAINT df_lp_favorite DEFAULT 0,
            favorited_at DATETIME2 NULL;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE object_id = OBJECT_ID('lesson_plans') 
      AND name = 'ix_lp_favorite'
)
BEGIN
    CREATE INDEX ix_lp_favorite
        ON lesson_plans(is_favorite)
        WHERE is_favorite = 1;
END
GO
