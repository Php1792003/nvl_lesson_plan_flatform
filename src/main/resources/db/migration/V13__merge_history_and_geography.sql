-- ============================================================
-- V13__merge_history_and_geography.sql
-- Gộp Lịch sử + Địa lý → Lịch sử và Địa lý
-- ============================================================

-- Bước 1: Đổi tên Lịch sử thành Lịch sử và Địa lý
UPDATE departments SET name = N'Lịch sử và Địa lý' WHERE name = N'Lịch sử';

-- Bước 2: Chuyển user và lesson plan từ Địa lý sang Lịch sử và Địa lý
UPDATE users
SET department_id = (SELECT id FROM departments WHERE name = N'Lịch sử và Địa lý')
WHERE department_id = (SELECT id FROM departments WHERE name = N'Địa lý');

UPDATE lesson_plans
SET department_id = (SELECT id FROM departments WHERE name = N'Lịch sử và Địa lý')
WHERE department_id = (SELECT id FROM departments WHERE name = N'Địa lý');

-- Bước 3: Xoá môn Địa lý cũ
DELETE FROM departments WHERE name = N'Địa lý';

