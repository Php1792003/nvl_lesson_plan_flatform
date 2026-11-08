-- ==============================================================================
-- Migration V18: Lưu trữ phản hồi chi tiết từ Cổng API Nhà phát hành chữ ký số
-- Phục vụ hiển thị chính xác kết quả phản hồi lên UI và lưu trữ kiểm toán hệ thống
-- ==============================================================================

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('nq52_user_signature_profiles') AND name = 'last_ca_response')
BEGIN
    ALTER TABLE nq52_user_signature_profiles ADD last_ca_response NVARCHAR(MAX) NULL;
END;

