-- ==============================================================================
-- Migration V19: Thêm trường linked_at để theo dõi thời điểm liên kết tài khoản ký số
-- Xác thực đúng 1 lần và có hiệu lực trong 7 ngày, sau 7 ngày yêu cầu liên kết lại
-- ==============================================================================

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('nq52_user_signature_profiles') AND name = 'linked_at')
BEGIN
    ALTER TABLE nq52_user_signature_profiles ADD linked_at DATETIME2 NULL;
END;
