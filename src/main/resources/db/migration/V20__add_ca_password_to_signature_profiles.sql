-- ==============================================================================
-- Migration V20: Thêm trường ca_password để lưu mật khẩu tài khoản chữ ký số đã liên kết
-- Phục vụ ký số từ xa tự động qua Cổng API nhà phát hành (VNPT SmartCA, Viettel Cloud-CA, VNeID)
-- ==============================================================================

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('nq52_user_signature_profiles') AND name = 'ca_password')
BEGIN
    ALTER TABLE nq52_user_signature_profiles ADD ca_password NVARCHAR(255) NULL;
END;

