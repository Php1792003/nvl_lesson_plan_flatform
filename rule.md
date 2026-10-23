# PROJECT RULES — Hệ thống quản lý giáo án (Lesson Plan Management)

> File này là nguồn sự thật (source of truth) cho toàn bộ dự án. Mọi code, migration, template phải tuân theo.
> Nếu cần đổi quy tắc, sửa file này trước rồi mới sửa code.

---

## 1. Mục tiêu và phạm vi

Website cho một trường học để giáo viên tải và quản lý giáo án; Ban giám hiệu (BGH) xem toàn bộ giáo án và đánh giá trực tiếp trên từng giáo án.

**Ngoài phạm vi (không làm):**
- Không có luồng duyệt giáo án: **không có trạng thái APPROVED / REJECTED / NEEDS_REVISION, không chấm điểm, không rubric**. BGH chỉ **xem** và **đánh giá (nhận xét)**.
- Không có soft delete cho giáo viên và giáo án (xem mục 4.5).

## 2. Công nghệ

| Hạng mục | Lựa chọn |
|---|---|
| Ngôn ngữ / Framework | Java 17+, Spring Boot 3.x |
| View | Thymeleaf + `thymeleaf-extras-springsecurity6` (server-side rendering) |
| Bảo mật | Spring Security, JWT (jjwt), BCrypt (strength >= 12) hoặc Argon2 |
| Database | SQL Server (mssql-jdbc), Spring Data JPA/Hibernate |
| Migration | Flyway (bắt buộc, `ddl-auto=validate`, không dùng `update`) |
| Email | Spring Mail + template Thymeleaf |
| Rate limit | Bucket4j |
| Phát hiện loại file | Apache Tika (kiểm tra magic bytes) |
| Biểu đồ | Chart.js, **tải file về host local** (không dùng CDN, để CSP chặt) |
| Test | JUnit 5, Spring Security Test, Testcontainers (SQL Server) |

## 3. Vai trò và phân quyền

### 3.1 Vai trò
- `ADMIN` — Ban giám hiệu. Tài khoản đầu tiên được seed bằng migration/biến môi trường, **không tự đăng ký**.
- `TEACHER` — Giáo viên, tự đăng ký.
- BGH có thể **tạo thêm vai trò** (ví dụ Tổ trưởng chuyên môn) và gán quyền cho vai trò đó.

### 3.2 RBAC động
- Bảng `roles`, `permissions`, `role_permissions`, `user_roles`.
- Quyền tính theo `permission code`, kiểm tra bằng `@PreAuthorize("hasAuthority('...')")`.
- Quyền được **tải từ DB** (cache ngắn, tối đa vài phút), **không tin quyền nằm trong JWT**, để đổi vai trò có hiệu lực nhanh.

Danh sách permission tối thiểu:

```
USER_VIEW, USER_CREATE, USER_UPDATE, USER_DELETE, USER_LOCK
ROLE_MANAGE
PLAN_VIEW_ALL
EVALUATION_CREATE
DEPARTMENT_MANAGE         (quản lý bộ môn)
CAMPUS_MANAGE             (quản lý phân hiệu)
AUDIT_VIEW
```

`ADMIN` có toàn bộ. `TEACHER` mặc định không có permission nào ở trên; quyền với giáo án của chính mình được xử lý bằng quy tắc sở hữu (mục 4.3), không phải permission.

## 4. Quy tắc nghiệp vụ (BẮT BUỘC)

### 4.1 Tài khoản và đăng ký
- Form đăng ký: **Họ** (`last_name`), **Tên đệm và tên** (`given_name`), **Bộ môn** (select từ bảng `departments`), **Email**, **Điểm trường / Phân hiệu** (select từ bảng `campuses`, mặc định **Điểm trường chính**), **Mật khẩu**, **Xác nhận mật khẩu**.
- Email duy nhất (không phân biệt hoa thường), chuẩn hoá `lower(trim())`.
- Trạng thái tài khoản: `PENDING_EMAIL → PENDING_APPROVAL → ACTIVE`, và `LOCKED`.
  - Đăng ký xong: `PENDING_EMAIL`, hệ thống gửi mã xác thực.
  - Nhập đúng mã: `PENDING_APPROVAL`, chờ BGH duyệt.
  - BGH duyệt: `ACTIVE`. Chỉ `ACTIVE` mới đăng nhập được.
  - BGH khoá: `LOCKED` (kèm `lock_reason`, `locked_at`). Tài khoản bị khoá **mất hiệu lực ngay lập tức**, kể cả khi token chưa hết hạn.
- Có: quên mật khẩu, đổi mật khẩu, sửa hồ sơ cá nhân.

### 4.2 Xác thực email (OTP)
- Mã 6 chữ số, sinh bằng `SecureRandom`.
- Lưu **hash** của mã (không lưu mã thô), hết hạn **10 phút**.
- Tối đa **5 lần nhập sai** cho mỗi mã; quá số lần thì phải xin mã mới.
- Gửi lại mã: cách nhau tối thiểu **60 giây**, tối đa 5 lần/giờ/email.
- Mã chỉ dùng **một lần** (`consumed_at`).

### 4.3 Quyền xem và truy cập giáo án (quan trọng nhất)

```
canView(user, plan) =
       user có PLAN_VIEW_ALL (ADMIN)
    OR plan.owner_id == user.id
    OR EXISTS share(plan.id, user.id) với revoked_at IS NULL
                                       và (expires_at IS NULL hoặc expires_at > now)
```

- Quyền sửa / xoá / chia sẻ / thu hồi chia sẻ: **chỉ chủ giáo án** (`owner_id == user.id`).
- Người được chia sẻ: **chỉ xem và tải**; **không được chia sẻ tiếp**.
- BGH xem được **tất cả** giáo án (kể cả giáo án không còn chủ, xem 4.5), nhưng **không sửa nội dung file** của giáo viên.
- Kiểm tra quyền ở **tầng Service** (dùng `PermissionEvaluator` hoặc service `LessonPlanAccessService`), **không chỉ ẩn nút ở giao diện**.
- Người không có quyền nhận **404** (không phải 403) để không lộ việc giáo án có tồn tại.
- Mọi endpoint nhận `planId`, `versionId`, `evaluationId`... phải kiểm tra quyền trên **đối tượng đó** (chống IDOR). Không bao giờ tin id từ client.
- Danh sách giáo án: truy vấn đã **lọc quyền ở SQL** (owner OR shared), không lấy hết rồi lọc trong Java.
- Ghi `audit_logs` mỗi lần: xem, tải, chia sẻ, thu hồi, xoá.

### 4.4 Giáo án và phiên bản
- Metadata: tiêu đề, bộ môn, khối/lớp, năm học, học kỳ, tuần/tiết, mô tả.
- Mỗi lần giáo viên thay file = **version mới** (`lesson_plan_versions`), giữ lịch sử; `lesson_plans.current_version_no` trỏ bản mới nhất. File của version cũ được dọn theo mục 4.7 để tiết kiệm dung lượng.
- Đánh giá của BGH gắn với **version** tại thời điểm đánh giá.
- Giáo viên **thêm / sửa / xoá** giáo án của mình với điều kiện ở 4.5.

### 4.5 Quy tắc xoá (không dùng soft delete)

**a) BGH xoá giáo viên (xoá thật dòng `users`):**
- **Giáo án của giáo viên đó được GIỮ LẠI** (cả file lẫn version). **Đánh giá KHÔNG bị xoá.**
- Trong cùng một transaction, trước khi xoá user:
  1. Ghi **snapshot** vào mọi giáo án của user: `owner_snapshot_name`, `owner_snapshot_email`, `owner_snapshot_department`, `owner_snapshot_campus`.
  2. `lesson_plans.owner_id = NULL`.
  3. `evaluation_replies.author_id = NULL` (giữ `author_name_snapshot`).
  4. `lesson_plan_shares`: xoá các dòng có `shared_with_user_id = user.id`; các share mà user đã cấp cho người khác **giữ nguyên** (đồng nghiệp vẫn xem được giáo án); `granted_by = NULL`.
  5. Xoá: `refresh_tokens`, `email_verification_codes`, `password_reset_tokens`, `notifications` của user và các dòng `email_outbox` đang chờ gửi tới email đó.
  6. Ghi `audit_logs` (không FK tới user, có `actor_email_snapshot`).
- Giáo án mồ côi (`owner_id IS NULL`): hiển thị "*Giáo viên đã bị xoá — {tên snapshot}*". Chỉ BGH và người đã được chia sẻ xem được. **Không ai sửa/xoá được** giáo án mồ côi trên giao diện (giữ nguyên làm hồ sơ).
- Không gửi email đánh giá cho giáo án mồ côi (không có người nhận).
- Không cho BGH xoá chính mình, và không cho xoá **ADMIN cuối cùng**.
- UI xoá phải yêu cầu **xác nhận** (nhập lại email của giáo viên) và nói rõ giáo án được giữ lại.
- Khuyến nghị: dùng **khoá tài khoản** cho trường hợp tạm ngưng; xoá chỉ cho trường hợp thật sự cần.

**b) Giáo viên xoá giáo án của mình:**
- **Giáo án đã có đánh giá thì KHÔNG được xoá** (chỉ được tải version mới). Lý do: không được làm mất đánh giá.
- Giáo án **chưa có đánh giá** được xoá thật: xoá dòng DB, các version, các share, và file trên đĩa.

**c) Đánh giá (evaluations):**
- **Không có chức năng xoá đánh giá** ở bất kỳ vai trò nào. FK từ `evaluations` luôn là `NO ACTION` (không cascade xoá).
- BGH được sửa đánh giá của chính mình (hiển thị "đã chỉnh sửa", có `edited_at`); sửa **không** gửi email lại.

### 4.6 Đánh giá của Ban giám hiệu
- BGH xem giáo án và **viết nhận xét** (text). Không trạng thái, không điểm.
- Mỗi nhận xét mới: ghi DB + tạo **email gửi giáo viên chủ giáo án** (giống Jira comment) + tạo thông báo trong hệ thống. Email có link tới giáo án (bấm vào phải đăng nhập).
- Chủ giáo án được **trả lời** nhận xét (`evaluation_replies`); khi có trả lời thì gửi email/thông báo cho BGH đã viết nhận xét.
- Ai thấy đánh giá: **chỉ chủ giáo án và BGH**. Người được chia sẻ **không** thấy đánh giá.
- Nội dung nhận xét được escape khi hiển thị (`th:text`), không dùng `th:utext`.

### 4.7 Tiết kiệm dung lượng lưu trữ

File Word/PowerPoint khá nặng và mỗi lần thay file tạo một version mới, nên bắt buộc áp dụng:

- **Giới hạn file:** tối đa **10 MB**/file (`APP_UPLOAD_MAX_MB`).
- **Chống trùng:** nếu `sha256` của file mới **trùng version hiện tại** của cùng giáo án thì từ chối ("File giống bản hiện tại"), không tạo version mới.
- **Dọn file của version cũ (purge):**
  - Giữ **file** của **3 version mới nhất** (`APP_KEEP_VERSIONS`) **và mọi version đã có đánh giá** của BGH.
  - Các version còn lại: **xoá file vật lý**, nhưng **giữ dòng DB** (đặt `file_purged_at`). **Không bao giờ xoá dòng version hay đánh giá.**
  - Job chạy mỗi ngày (ví dụ 02:00), ghi `audit_logs` với action `FILE_PURGED`.
  - Version đã dọn hiển thị "File đã được dọn để tiết kiệm dung lượng"; tải về trả **410 Gone** với thông báo thân thiện.
- **File tạm:** upload ghi vào thư mục tạm, xoá nếu không hoàn tất sau 24 giờ. Job hằng tuần quét **file mồ côi** (có trên đĩa nhưng không có dòng DB) và xoá.
- **Giao diện:** trang upload hiển thị giới hạn dung lượng và nhắc nén ảnh trong Word/PowerPoint trước khi tải.
- **BGH:** có trang thống kê dung lượng theo giáo viên và theo phân hiệu.
- Không tự động chuyển sang PDF hay nén lại file (sẽ mất khả năng chỉnh sửa); để dành làm tuỳ chọn về sau.

## 5. Mô hình dữ liệu (SQL Server)

Quy ước: cột chữ dùng `NVARCHAR` (có dấu tiếng Việt), thời gian dùng `DATETIME2` lưu **UTC**, khoá chính `BIGINT IDENTITY`, collation tìm kiếm `Vietnamese_CI_AI`.

```
campuses(id, name UNIQUE, is_default BIT)               -- seed: "Điểm trường chính" is_default=1, "Phân hiệu 1", "Phân hiệu 2"
departments(id, name UNIQUE)

users(id, last_name, given_name, email UNIQUE, password_hash,
      department_id FK, campus_id FK,
      status, lock_reason NULL, locked_at NULL,
      token_version INT, failed_login_count INT, locked_until NULL,
      created_at, updated_at)

roles(id, code UNIQUE, name)
permissions(id, code UNIQUE)
role_permissions(role_id, permission_id)
user_roles(user_id, role_id)

email_verification_codes(id, user_id FK, code_hash, expires_at, attempts, last_sent_at, consumed_at NULL)
password_reset_tokens(id, user_id FK, token_hash, expires_at, consumed_at NULL)
refresh_tokens(id, user_id FK, token_hash UNIQUE, family_id, expires_at,
               revoked_at NULL, replaced_by_id NULL, ip, user_agent, created_at)

lesson_plans(id,
             owner_id NULL FK users ON DELETE SET NULL,      -- NULL = giáo án mồ côi
             owner_snapshot_name NULL, owner_snapshot_email NULL,
             owner_snapshot_department NULL, owner_snapshot_campus NULL,
             title, department_id FK, grade, school_year, semester, week_no NULL,
             description NULL, current_version_no INT, created_at, updated_at)

lesson_plan_versions(id, plan_id FK, version_no, original_filename,
                     stored_filename UNIQUE,                 -- UUID
                     content_type, size_bytes, sha256, uploaded_at,
                     file_purged_at NULL,                    -- file vật lý đã bị dọn (mục 4.7)
                     UNIQUE(plan_id, version_no))

lesson_plan_shares(id, plan_id FK, shared_with_user_id FK, permission,   -- 'VIEW'
                   granted_by NULL, granted_at, expires_at NULL, revoked_at NULL)
   -- UNIQUE INDEX (plan_id, shared_with_user_id) WHERE revoked_at IS NULL

evaluations(id, plan_id FK NO ACTION, version_id FK NO ACTION,
            reviewer_id NULL FK users ON DELETE SET NULL, reviewer_name_snapshot,
            content, created_at, edited_at NULL)

evaluation_replies(id, evaluation_id FK NO ACTION,
                   author_id NULL FK users ON DELETE SET NULL, author_name_snapshot,
                   content, created_at)

notifications(id, user_id FK, type, title, link, read_at NULL, created_at)

email_outbox(id, to_email, subject, template, payload_json,
             status,                                         -- PENDING | SENT | FAILED
             attempts, next_attempt_at, last_error NULL, created_at, sent_at NULL)

audit_logs(id, actor_user_id NULL (KHÔNG FK), actor_email_snapshot,
           action, target_type, target_id NULL, ip, detail NULL, created_at)
```

Index bắt buộc: `users(email)`, `users(status)`, `lesson_plans(owner_id, created_at)`, `lesson_plans(department_id, created_at)`, `lesson_plan_shares(shared_with_user_id)`, `evaluations(plan_id, created_at)`, `email_outbox(status, next_attempt_at)`, `refresh_tokens(family_id)`.

> `ON DELETE SET NULL` chỉ là lưới an toàn; **service vẫn phải snapshot + set NULL tường minh** (mục 4.5a) trước khi xoá user.

## 6. Bảo mật

### 6.1 JWT và cookie
- **Access token**: sống ~15 phút. **Refresh token**: ~7 ngày. Cả hai chỉ nằm trong **cookie**, không bao giờ ở `localStorage`, URL hay response body.
- Thuộc tính cookie: `HttpOnly; Secure; SameSite=Lax; Path=/` (access), `Path=/auth` (refresh).
  - Dùng `Lax` (không dùng `Strict`) vì email gửi link tới giáo án; `Strict` sẽ làm người dùng bị coi như chưa đăng nhập khi bấm link từ email.
- JWT ký **HS256 với secret >= 256 bit từ biến môi trường** (hoặc RS256). Claims: `sub`, `jti`, `iat`, `exp`, `tv` (token_version). Không nhét thông tin nhạy cảm.
- Mỗi request: filter xác thực chữ ký + hạn + kiểm tra `users.status == ACTIVE` và `tv == users.token_version`. Khoá tài khoản / đổi mật khẩu / đổi vai trò quan trọng → tăng `token_version`.
- **Refresh token rotation**: mỗi lần refresh cấp token mới, token cũ bị thu hồi (`replaced_by_id`). Lưu **hash** trong DB. Nếu một token đã bị thu hồi mà bị dùng lại (**reuse detection**) → thu hồi cả `family_id` và buộc đăng nhập lại.
- Logout: thu hồi refresh token + xoá cookie.
- **CSRF bật** cho mọi request thay đổi dữ liệu (POST/PUT/PATCH/DELETE). Thymeleaf tự chèn token vào form. GET phải an toàn (không thay đổi dữ liệu).
- Production **chỉ HTTPS**: redirect HTTP→HTTPS, HSTS (`max-age>=1 năm`, `includeSubDomains`).

### 6.2 Đăng nhập và mật khẩu
- Mật khẩu tối thiểu 8 ký tự, có chữ và số; hash BCrypt/Argon2.
- Thông báo lỗi đăng nhập **chung chung** ("Email hoặc mật khẩu không đúng"), không lộ email có tồn tại hay không.
- Rate limit: đăng nhập, đăng ký, gửi OTP, quên mật khẩu (theo IP **và** theo email). Sai quá 5 lần → khoá tạm (`locked_until`).
- Có CAPTCHA hoặc rate limit chặt cho form đăng ký.

### 6.3 Upload và tải file
- Cho phép: `pdf, doc, docx`. Giới hạn **10 MB**/file (xem mục 4.7).
- Kiểm tra **cả đuôi, MIME khai báo và magic bytes** (Tika). Sai một trong ba → từ chối.
- Tên file lưu trên đĩa là **UUID ngẫu nhiên**; `original_filename` chỉ để hiển thị (làm sạch ký tự đặc biệt, chống path traversal).
- Thư mục lưu **nằm ngoài web root** (`app.storage.root`), không map thành static resource.
- Tải file **luôn qua controller** sau khi `canView`: `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`.
- Tính và lưu `sha256`. (Tuỳ chọn: quét ClamAV.)

### 6.4 Header, XSS, khác
- CSP chặt (`default-src 'self'`; script chỉ từ `self`), `X-Frame-Options: DENY`, `Referrer-Policy: same-origin`.
- Thymeleaf luôn `th:text`; cấm `th:utext` với dữ liệu người dùng.
- Dùng JPA/parameterized query, **cấm nối chuỗi SQL**.
- Secrets (DB, JWT secret, SMTP) đọc từ biến môi trường; **không commit** vào git.
- **Không log** mật khẩu, token, OTP, nội dung giáo án; log email thì che bớt.
- Tuân thủ bảo vệ dữ liệu cá nhân (Nghị định 13/2023/NĐ-CP): thu thập tối thiểu, giới hạn truy cập, có audit log.

## 7. Email (kiểu Jira)

- Dùng **Transactional Outbox**: khi BGH tạo đánh giá, ghi `evaluations` và `email_outbox` trong **cùng một transaction**.
- Job `@Scheduled` (mỗi ~30 giây) lấy `PENDING` đến hạn, gửi, cập nhật `SENT`; lỗi thì tăng `attempts` và backoff (1 → 5 → 15 → 60 phút), tối đa 5 lần rồi `FAILED`.
- Gửi email lỗi **không được** làm mất đánh giá hay làm lỗi request của người dùng.
- Loại email: mã xác thực, tài khoản được duyệt, đặt lại mật khẩu, **có đánh giá mới**, **có trả lời đánh giá**, **được chia sẻ giáo án**.
- Template ở `templates/mail/*.html`, tiếng Việt, có link sâu tới giáo án.
- Không gửi email cho giáo án mồ côi hoặc tài khoản không còn tồn tại.

## 8. Dashboard

- **Giáo viên**: tổng giáo án của mình, số giáo án theo tháng (12 tháng gần nhất), theo năm học/học kỳ. Chỉ số liệu của chính mình.
- **BGH**: tổng giáo án toàn trường (**gồm cả giáo án mồ côi**), theo tháng, theo bộ môn, theo phân hiệu; bộ lọc năm học / phân hiệu / bộ môn.
- Dữ liệu cho biểu đồ lấy từ endpoint JSON (`/api/dashboard/...`), dùng `GROUP BY` với index phù hợp; vẫn xác thực bằng cookie và kiểm tra quyền.
- Chart.js phục vụ file local.

## 9. Quản lý tài khoản (BGH)

- Danh sách có **phân trang** (mặc định 20) và **tìm kiếm** theo họ tên, email, bộ môn, phân hiệu, trạng thái, vai trò.
- Tìm kiếm tiếng Việt không phân biệt hoa thường và dấu (`Vietnamese_CI_AI`); dùng tham số kiểu `N'...'`/`NVARCHAR`.
- Thao tác: thêm, sửa, **duyệt**, khoá / mở khoá (bắt buộc nhập lý do khi khoá), xoá (theo 4.5a), gán vai trò.
- Mọi thao tác ghi `audit_logs`.

## 10. Cấu trúc package

```
com.school.lessonplan
├── config/            # SecurityConfig, WebConfig, MailConfig, SchedulingConfig, properties
├── security/          # JwtService, JwtAuthFilter, CookieService, CurrentUser, PermissionEvaluator
├── auth/              # đăng ký, OTP, login/logout, refresh, quên mật khẩu
├── user/              # entity User, UserService, quản lý tài khoản (BGH), hồ sơ cá nhân
├── role/              # Role, Permission, RoleService
├── department/        # Department (bộ môn), DepartmentService
├── campus/            # Campus (phân hiệu), CampusService
├── lessonplan/        # LessonPlan, Version, LessonPlanService, AccessService, FileStorageService
├── share/             # LessonPlanShare, ShareService
├── evaluation/        # Evaluation, EvaluationReply, EvaluationService
├── notification/      # Notification
├── mail/              # EmailOutbox, MailSender job, template rendering
├── dashboard/         # DashboardService, DTO thống kê
├── audit/             # AuditLog, AuditService
└── common/            # exception, util, constants, validation

src/main/resources
├── db/migration/      # V1__init.sql, V2__seed.sql, ...
├── templates/         # layout, auth, dashboard, plans, shares, admin, mail, error
├── static/            # css, js, vendor/chartjs (local)
├── messages.properties (vi)
└── application.yml    # + application-prod.yml
```

Phân tầng: `Controller → Service → Repository`. Controller không chứa nghiệp vụ; Service giữ `@Transactional` và kiểm tra quyền; không trả **Entity** ra view (dùng DTO/ViewModel).

## 11. Route chính

```
GET  /register            POST /register
GET  /verify-email        POST /verify-email        POST /verify-email/resend
GET  /login               POST /login               POST /logout        POST /auth/refresh
GET  /forgot-password     POST /forgot-password     GET/POST /reset-password

GET  /dashboard
GET  /plans               (danh sách giáo án của tôi + được chia sẻ)
GET  /plans/new           POST /plans
GET  /plans/{id}          GET /plans/{id}/edit      POST /plans/{id}     POST /plans/{id}/delete
GET  /plans/{id}/versions/{vid}/download
POST /plans/{id}/shares   POST /plans/{id}/shares/{shareId}/revoke
POST /plans/{id}/evaluations/{eid}/replies

GET  /admin/plans         (BGH: tất cả giáo án, lọc/tìm kiếm)
POST /admin/plans/{id}/evaluations          POST /admin/plans/{id}/evaluations/{eid}/edit
GET  /admin/users         GET /admin/users/new      POST /admin/users
GET  /admin/users/{id}    POST /admin/users/{id}    POST /admin/users/{id}/approve
POST /admin/users/{id}/lock    POST /admin/users/{id}/unlock    POST /admin/users/{id}/delete
GET  /admin/roles         POST /admin/roles ...
GET  /admin/audit-logs
```

> Không có route xoá đánh giá.

## 12. Quy ước code

- Java 17+, constructor injection, không field injection; `final` khi có thể.
- Bean Validation (`@Valid`) ở mọi form/DTO; lỗi hiển thị bằng tiếng Việt qua `messages.properties`.
- Dữ liệu thời gian lưu UTC, hiển thị theo `Asia/Ho_Chi_Minh`.
- Mã hoá ký tự UTF-8 toàn bộ (file, DB, email).
- Có trang lỗi riêng cho 403 / 404 / 500, không lộ stacktrace.
- Mọi thay đổi schema = một file Flyway mới, **không sửa migration đã chạy**.
- Commit nhỏ, theo tính năng; không commit secrets, file upload, `application-local.yml`.

## 13. Test bắt buộc (phải pass trước khi merge)

1. **Ma trận quyền xem giáo án**: chủ xem được; người lạ **không** (404); người được share xem được; share hết hạn / bị thu hồi thì không; BGH xem tất cả.
2. **IDOR**: đổi `planId`/`versionId`/`evaluationId` trên URL không truy cập được dữ liệu người khác.
3. **Xoá giáo viên**: giáo án còn nguyên (owner NULL + snapshot), **đánh giá còn nguyên**, share giữa đồng nghiệp còn hiệu lực, file còn trên đĩa.
4. Giáo viên **không xoá được** giáo án đã có đánh giá; giáo án chưa có đánh giá xoá được (và file bị xoá).
5. Không tồn tại API/route nào xoá đánh giá.
6. OTP: hết hạn, sai quá 5 lần, dùng lại, gửi lại quá nhanh.
7. JWT: token hết hạn, sai chữ ký, `token_version` cũ, **tài khoản bị khoá phải bị chặn ngay**, refresh reuse → thu hồi cả family.
8. Upload: sai đuôi / sai magic bytes / quá dung lượng / tên file chứa `../`.
9. CSRF: POST không có token bị từ chối.
10. Outbox: lỗi SMTP không làm mất đánh giá; retry đúng backoff.
11. Dung lượng: file quá giới hạn bị chặn; file trùng version hiện tại bị từ chối; purge **không** xoá version có đánh giá và **không** xoá dòng DB/đánh giá; tải version đã dọn trả 410.

## 14. Cấu hình môi trường (ví dụ biến)

```
DB_URL, DB_USERNAME, DB_PASSWORD
JWT_SECRET                      # >= 256 bit
APP_BASE_URL                    # https://...
APP_STORAGE_ROOT                # thư mục ngoài web root
APP_UPLOAD_MAX_MB=10            # giới hạn mỗi file
APP_KEEP_VERSIONS=3             # số version mới nhất được giữ file
SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, MAIL_FROM
INITIAL_ADMIN_EMAIL, INITIAL_ADMIN_PASSWORD   # chỉ dùng seed lần đầu
```

## 15. Lộ trình triển khai (checklist)

- [ ] **Giai đoạn 1 — Nền tảng:** project Spring Boot, Flyway V1 (toàn bộ schema), seed campuses/departments/roles/permissions/admin đầu tiên, layout Thymeleaf cơ bản.
- [ ] **Giai đoạn 2 — Xác thực:** đăng ký, OTP, duyệt tài khoản, login/logout JWT cookie, refresh rotation, CSRF, quên/đổi mật khẩu, rate limit.
- [ ] **Giai đoạn 3 — Quản lý tài khoản (BGH):** danh sách/tìm kiếm/phân trang, thêm/sửa/duyệt/khoá/xoá (đúng 4.5a), RBAC động.
- [ ] **Giai đoạn 4 — Giáo án:** upload, version, danh sách, sửa, xoá (đúng 4.5b), tải file qua controller, `AccessService`, job dọn version cũ (4.7).
- [ ] **Giai đoạn 5 — Chia sẻ:** chia sẻ / thu hồi / hết hạn, danh sách "được chia sẻ với tôi", email thông báo chia sẻ.
- [ ] **Giai đoạn 6 — Đánh giá:** BGH xem + nhận xét, trả lời của giáo viên, outbox email, thông báo trong hệ thống.
- [ ] **Giai đoạn 7 — Dashboard:** thống kê giáo viên và BGH, bộ lọc.
- [ ] **Giai đoạn 8 — Hoàn thiện:** audit log UI, bộ test mục 13, HTTPS/HSTS/CSP, tài liệu triển khai, sao lưu DB và thư mục file.

## 16. Quyết định còn mở (đã chọn mặc định, đổi được)

| # | Nội dung | Mặc định |
|---|---|---|
| 1 | Người được chia sẻ có tải file không | Có (xem + tải) |
| 2 | Giáo viên trả lời nhận xét | Có |
| 3 | Duyệt tài khoản bởi BGH sau khi xác thực email | Có |
| 4 | Giáo viên xoá giáo án đã có đánh giá | Không cho phép |
| 5 | Giáo án mồ côi có thể bị BGH xoá | Không (giữ làm hồ sơ) |