-- ============================================================
-- V10__create_feedbacks_table.sql
-- Bảng lưu trữ ý kiến đóng góp & báo cáo sự cố của giáo viên gửi Ban Giám Hiệu
-- ============================================================

CREATE TABLE feedbacks (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id             BIGINT               NULL,
    title               NVARCHAR(250)        NOT NULL,
    category            NVARCHAR(50)         NOT NULL DEFAULT 'CHUYEN_MON',
    priority            NVARCHAR(30)         NOT NULL DEFAULT 'NORMAL',
    content             NVARCHAR(MAX)        NOT NULL,
    sender_name         NVARCHAR(200)        NOT NULL,
    sender_email        NVARCHAR(320)        NOT NULL,
    department_name     NVARCHAR(200)        NULL,
    status              NVARCHAR(30)         NOT NULL DEFAULT 'PENDING',
    admin_response      NVARCHAR(MAX)        NULL,
    responded_by_id     BIGINT               NULL,
    responded_at        DATETIME2            NULL,
    created_at          DATETIME2            NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2            NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT fk_feedbacks_user         FOREIGN KEY (user_id)         REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_feedbacks_responded_by FOREIGN KEY (responded_by_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE INDEX ix_feedbacks_user_id    ON feedbacks(user_id);
CREATE INDEX ix_feedbacks_status     ON feedbacks(status);
CREATE INDEX ix_feedbacks_category   ON feedbacks(category);
CREATE INDEX ix_feedbacks_created_at ON feedbacks(created_at);
