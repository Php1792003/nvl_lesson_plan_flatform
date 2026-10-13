-- ============================================================
-- V1__init.sql — Toàn bộ schema cho Hệ thống quản lý giáo án
-- Quy ước: NVARCHAR (tiếng Việt), DATETIME2 (UTC), BIGINT IDENTITY
-- ============================================================

-- Phân hiệu
CREATE TABLE campuses (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(200)   NOT NULL,
    is_default  BIT             NOT NULL DEFAULT 0,
    CONSTRAINT uq_campuses_name UNIQUE (name)
);

-- Bộ môn
CREATE TABLE departments (
    id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    CONSTRAINT uq_departments_name UNIQUE (name)
);

-- Người dùng
CREATE TABLE users (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    last_name           NVARCHAR(100)   NOT NULL,
    given_name          NVARCHAR(200)   NOT NULL,
    email               NVARCHAR(320)   NOT NULL,
    password_hash       NVARCHAR(500)   NOT NULL,
    department_id       BIGINT          NULL,
    campus_id           BIGINT          NULL,
    status              NVARCHAR(30)    NOT NULL DEFAULT 'PENDING_EMAIL',
    lock_reason         NVARCHAR(1000)  NULL,
    locked_at           DATETIME2       NULL,
    token_version       INT             NOT NULL DEFAULT 0,
    failed_login_count  INT             NOT NULL DEFAULT 0,
    locked_until        DATETIME2       NULL,
    created_at          DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_users_campus     FOREIGN KEY (campus_id)     REFERENCES campuses(id)
);

CREATE INDEX ix_users_email  ON users(email);
CREATE INDEX ix_users_status ON users(status);

-- Vai trò
CREATE TABLE roles (
    id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50)  NOT NULL,
    name NVARCHAR(200) NOT NULL,
    CONSTRAINT uq_roles_code UNIQUE (code)
);

-- Quyền
CREATE TABLE permissions (
    id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(100) NOT NULL,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

-- Vai trò — Quyền
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Người dùng — Vai trò
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Mã xác thực email (OTP)
CREATE TABLE email_verification_codes (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    code_hash    NVARCHAR(500) NOT NULL,
    expires_at   DATETIME2     NOT NULL,
    attempts     INT           NOT NULL DEFAULT 0,
    last_sent_at DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    consumed_at  DATETIME2     NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_evc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Token đặt lại mật khẩu
CREATE TABLE password_reset_tokens (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    code_hash   NVARCHAR(500) NOT NULL,
    expires_at  DATETIME2     NOT NULL,
    attempts    INT           NOT NULL DEFAULT 0,
    consumed_at DATETIME2     NULL,
    created_at  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Refresh token
CREATE TABLE refresh_tokens (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    token_hash      NVARCHAR(500)   NOT NULL,
    family_id       NVARCHAR(100)   NOT NULL,
    expires_at      DATETIME2       NOT NULL,
    revoked_at      DATETIME2       NULL,
    replaced_by_id  BIGINT          NULL,
    ip              NVARCHAR(50)    NULL,
    user_agent      NVARCHAR(500)   NULL,
    created_at      DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id)        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rt_replaced FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens(id)
);

CREATE INDEX ix_refresh_tokens_family ON refresh_tokens(family_id);

-- Giáo án
CREATE TABLE lesson_plans (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    owner_id                    BIGINT          NULL,
    owner_snapshot_name         NVARCHAR(400)   NULL,
    owner_snapshot_email        NVARCHAR(320)   NULL,
    owner_snapshot_department   NVARCHAR(200)   NULL,
    owner_snapshot_campus       NVARCHAR(200)   NULL,
    title                       NVARCHAR(500)   NOT NULL,
    department_id               BIGINT          NOT NULL,
    grade                       NVARCHAR(50)    NOT NULL,
    school_year                 NVARCHAR(20)    NOT NULL,
    semester                    INT             NOT NULL,
    week_no                     INT             NULL,
    period_no                   INT             NULL,
    description                 NVARCHAR(2000)  NULL,
    current_version_no          INT             NOT NULL DEFAULT 1,
    created_at                  DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at                  DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT fk_lp_owner      FOREIGN KEY (owner_id)      REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_lp_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE INDEX ix_lesson_plans_owner      ON lesson_plans(owner_id, created_at);
CREATE INDEX ix_lesson_plans_department ON lesson_plans(department_id, created_at);

-- Version giáo án
CREATE TABLE lesson_plan_versions (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    plan_id           BIGINT          NOT NULL,
    version_no        INT             NOT NULL,
    original_filename NVARCHAR(500)   NOT NULL,
    stored_filename   NVARCHAR(100)   NOT NULL,
    content_type      NVARCHAR(200)   NOT NULL,
    size_bytes        BIGINT          NOT NULL,
    sha256            NVARCHAR(64)    NOT NULL,
    uploaded_at       DATETIME2       NOT NULL DEFAULT SYSUTCDATETIME(),
    file_purged_at    DATETIME2       NULL,

    CONSTRAINT uq_lpv_stored_filename UNIQUE (stored_filename),
    CONSTRAINT uq_lpv_plan_version    UNIQUE (plan_id, version_no),
    CONSTRAINT fk_lpv_plan FOREIGN KEY (plan_id) REFERENCES lesson_plans(id) ON DELETE CASCADE
);

-- Chia sẻ giáo án
CREATE TABLE lesson_plan_shares (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    plan_id              BIGINT       NOT NULL,
    shared_with_user_id  BIGINT       NOT NULL,
    permission           NVARCHAR(20) NOT NULL DEFAULT 'VIEW',
    granted_by           BIGINT       NULL,
    granted_at           DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    expires_at           DATETIME2    NULL,
    revoked_at           DATETIME2    NULL,

    CONSTRAINT fk_lps_plan    FOREIGN KEY (plan_id)             REFERENCES lesson_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_lps_shared  FOREIGN KEY (shared_with_user_id) REFERENCES users(id),
    CONSTRAINT fk_lps_granted FOREIGN KEY (granted_by)          REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_lps_shared_with ON lesson_plan_shares(shared_with_user_id);

-- Unique index cho active share (chỉ 1 share active mỗi user-plan)
CREATE UNIQUE INDEX uix_lps_active_share
    ON lesson_plan_shares(plan_id, shared_with_user_id)
    WHERE revoked_at IS NULL;

-- Đánh giá của BGH
CREATE TABLE evaluations (
    id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
    plan_id                BIGINT         NOT NULL,
    version_id             BIGINT         NOT NULL,
    reviewer_id            BIGINT         NULL,
    reviewer_name_snapshot NVARCHAR(400)  NOT NULL,
    content                NVARCHAR(4000) NOT NULL,
    created_at             DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    edited_at              DATETIME2      NULL,

    CONSTRAINT fk_eval_plan     FOREIGN KEY (plan_id)    REFERENCES lesson_plans(id),
    CONSTRAINT fk_eval_version  FOREIGN KEY (version_id) REFERENCES lesson_plan_versions(id),
    CONSTRAINT fk_eval_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_evaluations_plan ON evaluations(plan_id, created_at);

-- Trả lời đánh giá
CREATE TABLE evaluation_replies (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    evaluation_id        BIGINT         NOT NULL,
    author_id            BIGINT         NULL,
    author_name_snapshot NVARCHAR(400)  NOT NULL,
    content              NVARCHAR(4000) NOT NULL,
    created_at           DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT fk_er_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations(id),
    CONSTRAINT fk_er_author     FOREIGN KEY (author_id)     REFERENCES users(id) ON DELETE SET NULL
);

-- Thông báo
CREATE TABLE notifications (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    type       NVARCHAR(50)   NOT NULL,
    title      NVARCHAR(500)  NOT NULL,
    link       NVARCHAR(500)  NULL,
    read_at    DATETIME2      NULL,
    created_at DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Email outbox
CREATE TABLE email_outbox (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    to_email        NVARCHAR(320)  NOT NULL,
    subject         NVARCHAR(500)  NOT NULL,
    template        NVARCHAR(200)  NOT NULL,
    payload_json    NVARCHAR(MAX)  NULL,
    status          NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    attempts        INT            NOT NULL DEFAULT 0,
    next_attempt_at DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    last_error      NVARCHAR(2000) NULL,
    created_at      DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    sent_at         DATETIME2      NULL
);

CREATE INDEX ix_email_outbox_status ON email_outbox(status, next_attempt_at);

-- Audit log
CREATE TABLE audit_logs (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    actor_user_id        BIGINT         NULL,
    actor_email_snapshot NVARCHAR(320)  NULL,
    action               NVARCHAR(100)  NOT NULL,
    target_type          NVARCHAR(100)  NULL,
    target_id            BIGINT         NULL,
    ip                   NVARCHAR(50)   NULL,
    detail               NVARCHAR(MAX)  NULL,
    created_at           DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);
