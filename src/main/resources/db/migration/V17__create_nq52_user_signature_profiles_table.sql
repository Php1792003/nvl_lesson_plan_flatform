-- ==============================================================================
-- Migration V17: Quản lý Hồ sơ & Con dấu Chữ ký số cá nhân theo Nghị quyết 52
-- Phục vụ đăng nhập tài khoản chữ ký số và dán chữ ký vào Mẫu 01TKNV & Mẫu 03
-- ==============================================================================

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'nq52_user_signature_profiles')
BEGIN
    CREATE TABLE nq52_user_signature_profiles (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,

        -- Thông tin nhân sự
        moet_id NVARCHAR(50),                   -- Mã định danh Bộ GD&ĐT
        full_name NVARCHAR(255),               -- Họ tên
        dob NVARCHAR(50),                      -- Ngày sinh (dd/MM/yyyy)
        gender NVARCHAR(20) DEFAULT N'Nam',    -- Giới tính (Nam/Nữ)
        national_id NVARCHAR(50),              -- CMND/CCCD
        personal_id NVARCHAR(50),              -- Số định danh cá nhân

        -- Thông tin đăng ký tài khoản chữ ký số
        registration_status NVARCHAR(100) DEFAULT N'Đã duyệt tài khoản',
        signature_type NVARCHAR(100) DEFAULT N'REMOTE SIGNING',
        ca_provider NVARCHAR(255) DEFAULT N'Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)',
        ca_username NVARCHAR(255),

        -- Thông tin đăng ký chứng thư số
        cert_serial NVARCHAR(100),
        cert_subject NVARCHAR(500),
        valid_from NVARCHAR(50),
        valid_to NVARCHAR(50),
        owner_info NVARCHAR(500),

        -- Thông tin hình ảnh chữ ký
        signature_image_base64 NVARCHAR(MAX),  -- Ảnh chữ ký hoặc con dấu PNG base64
        auto_attach_signature BIT DEFAULT 1,   -- Tự động dán chữ ký khi ký văn bản NQ 52

        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

        CONSTRAINT fk_nq52_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE INDEX idx_nq52_profile_user ON nq52_user_signature_profiles(user_id);
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('nq52_digital_signatures') AND name = 'signature_image_base64')
BEGIN
    ALTER TABLE nq52_digital_signatures ADD signature_image_base64 NVARCHAR(MAX) NULL;
END;

