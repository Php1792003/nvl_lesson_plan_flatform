-- ==============================================================================
-- Migration V16: Quản lý Chữ ký số điện tử theo Nghị quyết 52
-- Hỗ trợ Remote Signing (Giáo viên, Tổ trưởng) & USB Token (Ban Giám Hiệu)
-- ==============================================================================

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'nq52_digital_signatures')
BEGIN
    CREATE TABLE nq52_digital_signatures (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        
        -- Phân loại chứng từ: 'WORK_RESULT' (Mẫu 01TKNV) hoặc 'EVALUATION' (Mẫu 03)
        document_type NVARCHAR(50) NOT NULL,
        document_id BIGINT NOT NULL,
        
        -- Cấp ký & Vai trò: 'TEACHER', 'DEPT_LEADER', 'PRINCIPAL'
        signing_role NVARCHAR(50) NOT NULL,
        signer_id BIGINT NOT NULL,
        signer_name_snapshot NVARCHAR(255) NOT NULL,
        signer_position NVARCHAR(100) NOT NULL,
        work_unit NVARCHAR(300) NOT NULL DEFAULT N'Trường THCS Nguyễn Văn Linh',
        
        -- Phương thức ký: 'REMOTE_SIGNING' hoặc 'USB_TOKEN'
        signature_type NVARCHAR(50) NOT NULL,
        ca_provider NVARCHAR(100) NOT NULL, -- 'VNPT_SMARTCA', 'VIETTEL_CLOUD_CA', 'BAN_CO_YEU', 'VN_EID'...
        
        -- Thông tin Chứng thư số X.509
        cert_subject NVARCHAR(500),
        cert_serial NVARCHAR(100),
        cert_issuer NVARCHAR(500),
        cert_valid_from DATETIME2,
        cert_valid_to DATETIME2,
        
        -- Tính toàn vẹn dữ liệu
        data_hash_sha256 NVARCHAR(128) NOT NULL,
        signature_value NVARCHAR(MAX) NOT NULL,
        timestamp_token NVARCHAR(MAX),
        signed_at DATETIME2 NOT NULL,
        
        -- Nhật ký giao dịch & Kiểm toán
        transaction_id NVARCHAR(100),
        ip_address NVARCHAR(50),
        user_agent NVARCHAR(500),
        status NVARCHAR(30) NOT NULL DEFAULT 'VALID', -- 'VALID', 'REVOKED'
        revocation_reason NVARCHAR(500),
        revoked_at DATETIME2,
        
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_nq52_sig_user FOREIGN KEY (signer_id) REFERENCES users(id) ON DELETE NO ACTION
    );

    CREATE INDEX idx_nq52_sig_doc ON nq52_digital_signatures(document_type, document_id);
    CREATE INDEX idx_nq52_sig_signer ON nq52_digital_signatures(signer_id);
    CREATE INDEX idx_nq52_sig_status ON nq52_digital_signatures(status);
END;

