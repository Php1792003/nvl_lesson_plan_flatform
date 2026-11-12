-- ============================================================
-- V12__rename_and_add_departments.sql
-- Đổi tên Vật lý/Hoá học/Sinh học → KHTN/KHTN1/KHTN2
-- Thêm KHTN3, Giáo dục địa phương, Hoạt động trải nghiệm hướng nghiệp
-- ============================================================

-- Đổi tên các môn Lý - Hoá - Sinh thành KHTN
UPDATE departments SET name = N'KHTN'  WHERE name = N'Vật lý';
UPDATE departments SET name = N'KHTN1' WHERE name = N'Hoá học';
UPDATE departments SET name = N'KHTN2' WHERE name = N'Sinh học';

-- Thêm các môn mới
INSERT INTO departments (name) VALUES (N'KHTN3');
INSERT INTO departments (name) VALUES (N'Giáo dục địa phương');
INSERT INTO departments (name) VALUES (N'Hoạt động trải nghiệm, hướng nghiệp');
