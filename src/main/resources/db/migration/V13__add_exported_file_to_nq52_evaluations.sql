-- ============================================================
-- V13__add_exported_file_to_nq52_evaluations.sql
-- Thêm các trường lưu trữ file Word Mẫu 03 đã xuất và lưu trong CSDL
-- ============================================================

ALTER TABLE nq52_evaluations
ADD exported_file_name     NVARCHAR(500)   NULL,
    exported_file_path     NVARCHAR(500)   NULL,
    exported_file_size     BIGINT          NULL,
    exported_file_type     NVARCHAR(100)   NULL,
    exported_at            DATETIME2       NULL,
    exported_file_data     VARBINARY(MAX)  NULL;
GO

