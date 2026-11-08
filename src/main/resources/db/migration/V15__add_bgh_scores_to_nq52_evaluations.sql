-- ============================================================
-- V15__add_bgh_scores_to_nq52_evaluations.sql
-- Thêm các trường lưu trữ điểm và nhận xét của Hội đồng thi đua, khen thưởng (Ban Giám Hiệu)
-- ============================================================

ALTER TABLE nq52_evaluations
ADD bgh_score_a             DECIMAL(5, 2)   NULL,
    bgh_score_b             DECIMAL(5, 2)   NULL,
    bgh_score_c_plus        DECIMAL(5, 2)   NULL,
    bgh_score_c_minus       DECIMAL(5, 2)   NULL,
    bgh_total_score         DECIMAL(5, 2)   NULL,
    bgh_classification      NVARCHAR(100)   NULL,
    bgh_notes               NVARCHAR(2000)  NULL,
    bgh_reviewer_id         BIGINT          NULL,
    bgh_reviewer_name_snapshot NVARCHAR(400) NULL,
    bgh_reviewed_at         DATETIME2       NULL;
GO

ALTER TABLE nq52_evaluations
ADD CONSTRAINT fk_nq52_bgh_reviewer FOREIGN KEY (bgh_reviewer_id) REFERENCES users(id) ON DELETE NO ACTION;
GO

