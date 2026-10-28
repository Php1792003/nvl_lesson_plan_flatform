# NVL Lesson Plan Platform

Nền tảng quản lý và thẩm định kế hoạch bài dạy (giáo án) dành cho trường THCS Nguyễn Văn Linh.

---

## 🛠 Công nghệ sử dụng

- **Backend**: Spring Boot 3, Java 17+, Spring Security, Spring Data JPA
- **Database**: Microsoft SQL Server
- **Migration**: Flyway (`db/migration`)
- **Frontend**: Thymeleaf, Tailwind CSS, Vanilla JS
- **AI Review**: Google Gemini API

---

## 🚀 Khởi chạy ứng dụng

### 1. Yêu cầu môi trường
- JDK 17 trở lên
- Microsoft SQL Server (Port 1433, Database: `lesson_plan_db`)

### 2. Cấu hình biến môi trường
Trước khi khởi động, cấu hình các biến môi trường cần thiết hoặc điều chỉnh trong `src/main/resources/application.yml`:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `GEMINI_API_KEY`: API Key dùng cho tính năng thẩm định giáo án bằng AI
- `SMTP_USERNAME`, `SMTP_PASSWORD`: Tài khoản gửi email OTP / thông báo

### 3. Chạy ứng dụng
- **Dùng Gradle**:
  ```bash
  ./gradlew bootRun
  ```
- **Dùng Makefile (nếu có make)**:
  ```bash
  make up       # Khởi chạy trong nền
  make logs     # Theo dõi log
  make down     # Dừng ứng dụng
  ```

- **Truy cập hệ thống**: [http://localhost:8080/portal](http://localhost:8080/portal)

---

## 📚 Tài liệu kỹ thuật chi tiết

Toàn bộ quy chuẩn phát triển và tài liệu đặc tả được lưu trữ trong thư mục `docs/`:

- [Tài liệu kiến trúc & Quy chuẩn phát triển](docs/rule.md)
- [Quy chuẩn giao diện Frontend](docs/rule_frontend.md)
- [Quy chuẩn kiểm tra dữ liệu (Validation Rules)](docs/VALIDATION-RULES.md)
- [Quy chuẩn Responsive & UI States](docs/UI-RESPONSIVE-RULES.md)
- [Cấu hình tải lên & AI Thẩm định](docs/AI_UPLOAD_OVERLOAD_CONFIGURATION.md)
