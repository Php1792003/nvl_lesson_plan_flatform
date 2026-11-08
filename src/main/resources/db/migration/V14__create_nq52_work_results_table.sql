-- ============================================================
-- V14__create_nq52_work_results_table.sql
-- Bảng thống kê, tự đánh giá kết quả thực hiện nhiệm vụ của cá nhân viên chức (Mẫu 01TKNV)
-- Kèm theo Phiếu đánh giá, xếp loại chất lượng hàng tháng của cá nhân theo Nghị quyết 52
-- ============================================================

CREATE TABLE nq52_work_results (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                     BIGINT               NOT NULL,
    department_id               BIGINT               NOT NULL,
    evaluation_month            INT                  NOT NULL,
    evaluation_year             INT                  NOT NULL,
    status                      NVARCHAR(30)         NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, REVIEWED

    -- Thông tin hiển thị tại thời điểm đánh giá
    teacher_name_snapshot       NVARCHAR(400)        NOT NULL,
    position_title              NVARCHAR(200)        NOT NULL DEFAULT N'Giáo viên',
    work_unit                   NVARCHAR(300)        NOT NULL DEFAULT N'Trường THCS Nguyễn Văn Linh',
    department_name_snapshot    NVARCHAR(200)        NOT NULL,
    campus_name_snapshot        NVARCHAR(200)        NULL,
    evaluation_date_str         NVARCHAR(100)        NULL,

    -- Bảng thống kê tổng số sản phẩm / công việc
    total_assigned              INT                  NOT NULL DEFAULT 0, -- (1) Được giao
    total_completed             INT                  NOT NULL DEFAULT 0, -- (2) Hoàn thành
    total_uncompleted           INT                  NOT NULL DEFAULT 0, -- (3) Chưa hoàn thành
    completion_rate             DECIMAL(5, 2)        NOT NULL DEFAULT 0.00, -- (4) Tỷ lệ hoàn thành %

    progress_early              INT                  NOT NULL DEFAULT 0, -- (5) Sớm hạn
    progress_on_time            INT                  NOT NULL DEFAULT 0, -- (6) Đúng hạn
    progress_late               INT                  NOT NULL DEFAULT 0, -- (7) Trễ hạn
    on_time_rate                DECIMAL(5, 2)        NOT NULL DEFAULT 0.00, -- (8) Tỷ lệ đúng/sớm hạn %

    quality_standard            INT                  NOT NULL DEFAULT 0, -- (9) Đảm bảo chất lượng
    quality_exceeded            INT                  NOT NULL DEFAULT 0, -- (10) Vượt mức yêu cầu
    quality_substandard         INT                  NOT NULL DEFAULT 0, -- (11) Chưa đảm bảo chất lượng
    quality_rate                DECIMAL(5, 2)        NOT NULL DEFAULT 0.00, -- (12) Tỷ lệ đảm bảo trở lên %

    summary_notes               NVARCHAR(2000)       NULL, -- (13) Ghi chú tổng hợp

    -- Dữ liệu chi tiết danh sách nhiệm vụ các phần I, II, III, IV dạng JSON
    tasks_data_json             NVARCHAR(MAX)        NULL,

    -- Đánh giá / Xác nhận của Tổ trưởng / Ban Giám Hiệu
    reviewer_id                 BIGINT               NULL,
    reviewer_name_snapshot      NVARCHAR(400)        NULL,
    reviewer_notes              NVARCHAR(2000)       NULL,

    submitted_at                DATETIME2            NULL,
    reviewed_at                 DATETIME2            NULL,

    -- Thông tin lưu trữ file Excel (.xlsx) xuất ra
    exported_file_name          NVARCHAR(500)        NULL,
    exported_file_path          NVARCHAR(500)        NULL,
    exported_file_size          BIGINT               NULL,
    exported_file_type          NVARCHAR(100)        NULL,
    exported_at                 DATETIME2            NULL,
    exported_file_data          VARBINARY(MAX)       NULL,

    created_at                  DATETIME2            NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at                  DATETIME2            NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT uq_nq52_wr_user_period UNIQUE (user_id, evaluation_month, evaluation_year),
    CONSTRAINT fk_nq52_wr_user       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nq52_wr_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_nq52_wr_reviewer   FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE INDEX ix_nq52_wr_user ON nq52_work_results(user_id);
CREATE INDEX ix_nq52_wr_dept_status ON nq52_work_results(department_id, status);
CREATE INDEX ix_nq52_wr_period ON nq52_work_results(evaluation_year, evaluation_month);
GO

