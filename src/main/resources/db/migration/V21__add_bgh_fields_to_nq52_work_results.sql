-- ============================================================
-- V21__add_bgh_fields_to_nq52_work_results.sql
-- Thêm các trường duyệt và nhận xét của Ban Giám Hiệu cho Bảng tự đánh giá kết quả (Mẫu 01TKNV)
-- Phục vụ quy trình duyệt 3 cấp: Giáo viên -> Tổ trưởng -> Ban Giám Hiệu
-- ============================================================

ALTER TABLE nq52_work_results ADD bgh_reviewer_id BIGINT NULL;
ALTER TABLE nq52_work_results ADD bgh_reviewer_name_snapshot NVARCHAR(400) NULL;
ALTER TABLE nq52_work_results ADD bgh_notes NVARCHAR(2000) NULL;
ALTER TABLE nq52_work_results ADD bgh_reviewed_at DATETIME2 NULL;

ALTER TABLE nq52_work_results ADD CONSTRAINT fk_nq52_wr_bgh_reviewer 
    FOREIGN KEY (bgh_reviewer_id) REFERENCES users(id) ON DELETE NO ACTION;
GO

