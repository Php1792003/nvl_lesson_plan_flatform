# VALIDATION RULES (BẮT BUỘC KHI GEN / SỬA FORM)

Áp dụng cho mọi form trong dự án (đăng ký, đăng nhập, OTP, quên/đặt lại mật khẩu, tạo/sửa user, khóa/xóa user, vai trò, tải giáo án, đánh giá...). Người dùng nhập sai PHẢI thấy ngay: **sai ở trường nào, vì sao, sửa thế nào**.

## 1. Nguyên tắc

1. **Validate 2 lớp**: client (để người dùng sửa nhanh) và server (bắt buộc, là nguồn sự thật). Cấm chỉ validate ở client.
2. **Lỗi hiện ngay dưới đúng trường bị sai**, bằng tiếng Việt, nói rõ cách sửa. Cấm chỉ hiện banner chung kiểu "Có lỗi xảy ra", cấm `alert()`.
3. **Không mất dữ liệu đã nhập** khi server trả lỗi (trừ mật khẩu).
4. **Mỗi trường hiện tối đa 1 thông báo** (lỗi đầu tiên theo thứ tự: bắt buộc, rồi định dạng, rồi nghiệp vụ).
5. **Tự focus vào trường lỗi đầu tiên** khi submit thất bại.
6. **Quy tắc client phải khớp server** (cùng độ dài, cùng regex, cùng thông báo). Khi đổi một bên phải đổi bên còn lại.
7. **Bảo mật**:
   - Đăng nhập, quên mật khẩu: dùng thông báo chung ("Email hoặc mật khẩu không đúng."), không cho biết email có tồn tại hay không.
   - Không đưa mật khẩu, OTP, token vào message, log, hay giá trị `value` của input.
   - Không trả stack trace hay thông báo kỹ thuật ra giao diện.
   - Luôn dùng `th:text` (không dùng `th:utext`) để hiển thị thông báo.
8. **Giao diện lỗi** phải tuân theo `UI-RESPONSIVE-RULES.md` (text lỗi tự xuống dòng, không làm vỡ layout ở 320px).

## 2. Kiến trúc phía server

### 2.1 Mỗi form POST = 1 DTO + `@Valid` + `BindingResult`

- Cấm dùng nhiều `@RequestParam` rời rạc cho form có từ 2 trường trở lên (khó validate, không giữ được dữ liệu, không gắn lỗi vào từng trường).
- DTO là **class thường có getter/setter** (không dùng `record` cho form-backing object vì Thymeleaf `th:field` cần getter dạng bean).
- Đặt trong package của tính năng, ví dụ `com.school.lessonplan.auth.dto.RegisterForm`.
- Cần có dependency `spring-boot-starter-validation`.

### 2.2 Thứ tự lỗi bằng `@GroupSequence`

Để mỗi trường chỉ có một lỗi tại một thời điểm (bắt buộc kiểm trước, định dạng sau):

```java
package com.school.lessonplan.common.validation;

public interface FormatGroup {}
```

Trên DTO: `@GroupSequence({RegisterForm.class, FormatGroup.class})`. Ràng buộc không khai báo `groups` thuộc nhóm bắt buộc (chạy trước); ràng buộc có `groups = FormatGroup.class` chỉ chạy khi nhóm bắt buộc đã đạt.

### 2.3 DTO mẫu (đăng ký giáo viên)

```java
package com.school.lessonplan.auth.dto;

import com.school.lessonplan.common.validation.FormatGroup;
import com.school.lessonplan.common.validation.PasswordMatches;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@GroupSequence({RegisterForm.class, FormatGroup.class})
@PasswordMatches(groups = FormatGroup.class)
public class RegisterForm {

    public static final String NAME_REGEX = "^[\\p{L}][\\p{L} .'’-]*$";
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    @NotBlank
    @Size(max = 50, groups = FormatGroup.class)
    @Pattern(regexp = NAME_REGEX, groups = FormatGroup.class)
    private String lastName;

    @NotBlank
    @Size(max = 80, groups = FormatGroup.class)
    @Pattern(regexp = NAME_REGEX, groups = FormatGroup.class)
    private String givenName;

    @NotBlank
    @Size(max = 150, groups = FormatGroup.class)
    @Pattern(regexp = EMAIL_REGEX, groups = FormatGroup.class)
    private String email;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long campusId;

    @NotBlank
    @Size(min = 8, max = 72, groups = FormatGroup.class)
    @Pattern(regexp = PASSWORD_REGEX, groups = FormatGroup.class)
    private String password;

    @NotBlank
    private String confirmPassword;

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName == null ? null : lastName.trim(); }
    public String getGivenName() { return givenName; }
    public void setGivenName(String givenName) { this.givenName = givenName == null ? null : givenName.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    // Setter của lastName, givenName, email có trim(). Setter của password, confirmPassword KHÔNG trim.
    // Không override toString() chứa password. Không log đối tượng này.
}
```

### 2.4 Validator so khớp mật khẩu (gắn lỗi vào đúng trường `confirmPassword`)

```java
package com.school.lessonplan.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchesValidator.class)
public @interface PasswordMatches {
    String message() default "{PasswordMatches}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
package com.school.lessonplan.common.validation;

import com.school.lessonplan.auth.dto.RegisterForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterForm> {

    @Override
    public boolean isValid(RegisterForm form, ConstraintValidatorContext ctx) {
        if (form.getPassword() == null || form.getConfirmPassword() == null) {
            return true; // lỗi bắt buộc đã do @NotBlank báo
        }
        boolean ok = form.getPassword().equals(form.getConfirmPassword());
        if (!ok) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("{PasswordMatches}")
               .addPropertyNode("confirmPassword")
               .addConstraintViolation();
        }
        return ok;
    }
}
```

Nếu nhiều form cần so khớp mật khẩu (đặt lại mật khẩu, đổi mật khẩu), tạo interface chung `PasswordConfirmable` có `getPassword()` và `getConfirmPassword()` rồi để validator dùng interface đó thay vì `RegisterForm`.

### 2.5 Controller mẫu

```java
@GetMapping("/register")
public String showRegister(Model model) {
    model.addAttribute("registerForm", new RegisterForm());
    loadRegisterLookups(model);
    return "auth/register-page"; // tên view thực tế của dự án
}

@PostMapping("/register")
public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                       BindingResult br,
                       Model model,
                       RedirectAttributes ra) {
    if (!br.hasErrors()) {
        try {
            authService.register(form);
            ra.addFlashAttribute("successMessage", "Đăng ký thành công. Vui lòng kiểm tra email để lấy mã xác thực.");
            return "redirect:/verify-email";
        } catch (FieldValidationException e) {
            br.rejectValue(e.getField(), e.getCode(), e.getMessage()); // lỗi nghiệp vụ gắn vào đúng trường
        } catch (BusinessException e) {
            br.reject("form.error", e.getMessage());                   // lỗi chung của form
        }
    }
    loadRegisterLookups(model); // nạp lại danh sách môn học, phân hiệu
    return "auth/register-page"; // trả lại chính trang form, KHÔNG redirect khi có lỗi (để giữ dữ liệu)
}
```

Quy tắc:
- **Có lỗi thì trả view trực tiếp** (không `redirect:`), nhớ nạp lại mọi dữ liệu dropdown mà view cần.
- **Thành công thì redirect** (Post-Redirect-Get) kèm `successMessage` dạng flash.
- Lỗi nghiệp vụ gắn vào trường (email trùng, mã OTP sai...) dùng `FieldValidationException`; lỗi chung dùng `br.reject(...)`.
- Với trang dùng `redirect:` bắt buộc (hiếm), truyền cả `BindingResult` qua flash: `ra.addFlashAttribute("org.springframework.validation.BindingResult.registerForm", br)` và `ra.addFlashAttribute("registerForm", form)` (xoá mật khẩu trước khi truyền).

```java
package com.school.lessonplan.common.exception;

public class FieldValidationException extends RuntimeException {
    private final String field;
    private final String code;

    public FieldValidationException(String field, String code, String message) {
        super(message);
        this.field = field;
        this.code = code;
    }

    public String getField() { return field; }
    public String getCode() { return code; }
}
```

Ví dụ trong service (email trùng), đồng thời bắt luôn lỗi trùng do race condition ở DB:

```java
if (userRepository.existsByEmailIgnoreCase(email)) {
    throw new FieldValidationException("email", "Duplicate", "Email đã được đăng ký.");
}
try {
    userRepository.save(user);
} catch (DataIntegrityViolationException ex) {
    throw new FieldValidationException("email", "Duplicate", "Email đã được đăng ký.");
}
```

### 2.6 Chuẩn hoá dữ liệu vào (trim)

- Các trường văn bản (họ, tên, email, lý do...) được cắt khoảng trắng đầu/cuối **ngay trong setter của DTO** (xem mẫu ở mục 2.3). Giữ chuỗi rỗng để `@NotBlank` báo lỗi "bắt buộc".
- **Không trim mật khẩu** và mã OTP (mật khẩu có thể chủ ý chứa khoảng trắng).
- Email chuyển về chữ thường khi lưu (trong service), không đổi giá trị người dùng đang thấy trên form.

### 2.7 API trả JSON (nếu có `@RestController`)

```java
package com.school.lessonplan.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ApiValidationExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Map<String, Object>> handle(BindException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Dữ liệu không hợp lệ.");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
```

## 3. Thông báo (`messages.properties`)

Dự án đã có `spring.messages.basename: messages` và `encoding: UTF-8`. **Cấm viết cứng `message = "..."` trong annotation**; mọi thông báo đặt trong `src/main/resources/messages.properties`.

Quy ước khoá (Spring tự tìm theo thứ tự từ cụ thể đến chung): `{Ràng buộc}.{tênForm}.{tênTrường}`.

```properties
# ===== Đăng ký (registerForm) =====
NotBlank.registerForm.lastName=Vui lòng nhập họ.
Size.registerForm.lastName=Họ không quá 50 ký tự.
Pattern.registerForm.lastName=Họ chỉ gồm chữ cái và khoảng trắng.

NotBlank.registerForm.givenName=Vui lòng nhập tên đệm và tên.
Size.registerForm.givenName=Tên đệm và tên không quá 80 ký tự.
Pattern.registerForm.givenName=Tên đệm và tên chỉ gồm chữ cái và khoảng trắng.

NotBlank.registerForm.email=Vui lòng nhập email.
Size.registerForm.email=Email không quá 150 ký tự.
Pattern.registerForm.email=Email không hợp lệ. Ví dụ: ten@school.edu.vn
Duplicate.registerForm.email=Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.

NotNull.registerForm.departmentId=Vui lòng chọn môn học.
NotNull.registerForm.campusId=Vui lòng chọn phân hiệu.

NotBlank.registerForm.password=Vui lòng nhập mật khẩu.
Size.registerForm.password=Mật khẩu cần từ 8 đến 72 ký tự.
Pattern.registerForm.password=Mật khẩu phải có cả chữ cái và chữ số.

NotBlank.registerForm.confirmPassword=Vui lòng nhập lại mật khẩu.
PasswordMatches=Mật khẩu xác nhận không khớp.

# ===== Lỗi chung / kiểu dữ liệu =====
typeMismatch=Giá trị không hợp lệ.
form.error=Không thể xử lý yêu cầu. Vui lòng thử lại.
```

### Bảng thông báo chuẩn (dùng lại cho các form khác)

| Tình huống | Thông báo |
|---|---|
| Trường bắt buộc để trống | `Vui lòng nhập {tên trường}.` (ô chọn: `Vui lòng chọn {tên trường}.`) |
| Quá dài | `{Tên trường} không quá {n} ký tự.` |
| Quá ngắn | `{Tên trường} cần ít nhất {n} ký tự.` |
| Sai định dạng email | `Email không hợp lệ. Ví dụ: ten@school.edu.vn` |
| Mật khẩu yếu | `Mật khẩu phải có cả chữ cái và chữ số.` |
| Xác nhận không khớp | `Mật khẩu xác nhận không khớp.` |
| Trùng dữ liệu | `{Tên trường} này đã được sử dụng.` |
| OTP sai định dạng | `Mã xác thực gồm 6 chữ số.` (điều chỉnh theo cấu hình) |
| OTP sai hoặc hết hạn | `Mã không đúng hoặc đã hết hạn. Bạn còn {n} lần thử.` |
| Đăng nhập sai | `Email hoặc mật khẩu không đúng.` (luôn dùng một câu chung) |
| Tài khoản chưa duyệt | `Tài khoản đang chờ Ban giám hiệu duyệt.` |
| Tài khoản bị khóa | `Tài khoản đã bị khóa. Vui lòng liên hệ Ban giám hiệu.` |
| Tệp sai định dạng | `Chỉ chấp nhận tệp PDF, Word hoặc PowerPoint.` |
| Tệp quá lớn | `Tệp vượt quá 10 MB. Hãy nén lại rồi thử lại.` |
| Vượt giới hạn thao tác | `Bạn thao tác quá nhanh. Vui lòng thử lại sau {n} phút.` |
| Xoá cần xác nhận email | `Email xác nhận không khớp.` |

Giọng văn: ngắn, lịch sự, nói rõ cách sửa, không dùng mã lỗi hay thuật ngữ kỹ thuật, không trách người dùng.

## 4. Giao diện (Thymeleaf)

### 4.1 Form

```html
<form th:action="@{/register}" th:object="${registerForm}" method="post" data-validate novalidate>
  <!-- th:action tự chèn _csrf. novalidate để dùng thông báo của dự án thay vì bong bóng mặc định của trình duyệt. -->

  <div class="form-alert" role="alert" th:if="${#fields.hasGlobalErrors()}" th:text="${#fields.globalErrors()[0]}"></div>

  <div class="form-group" th:classappend="${#fields.hasErrors('lastName')} ? 'has-error'">
    <label class="form-label" for="lastName">Họ <span class="req" aria-hidden="true">*</span></label>
    <input type="text" id="lastName" th:field="*{lastName}" class="form-control"
           maxlength="50" required autocomplete="family-name"
           data-pattern="^[\p{L}][\p{L} .'’-]*$"
           data-msg-required="Vui lòng nhập họ."
           data-msg-pattern="Họ chỉ gồm chữ cái và khoảng trắng."
           th:attr="aria-invalid=${#fields.hasErrors('lastName')},aria-describedby='lastName-error'">
    <p class="field-error" id="lastName-error" role="alert"
       th:if="${#fields.hasErrors('lastName')}" th:text="${#fields.errors('lastName')[0]}"></p>
  </div>

  <!-- Ô chọn -->
  <div class="form-group" th:classappend="${#fields.hasErrors('departmentId')} ? 'has-error'">
    <label class="form-label" for="departmentId">Môn học <span class="req" aria-hidden="true">*</span></label>
    <select id="departmentId" th:field="*{departmentId}" class="form-select" required
            data-msg-required="Vui lòng chọn môn học."
            th:attr="aria-invalid=${#fields.hasErrors('departmentId')},aria-describedby='departmentId-error'">
      <option value="">-- Chọn môn học --</option>
      <option th:each="d : ${departments}" th:value="${d.id}" th:text="${d.name}"></option>
    </select>
    <p class="field-error" id="departmentId-error" role="alert"
       th:if="${#fields.hasErrors('departmentId')}" th:text="${#fields.errors('departmentId')[0]}"></p>
  </div>

  <!-- Mật khẩu: Thymeleaf không điền lại giá trị cho input type=password -->
  <div class="form-group" th:classappend="${#fields.hasErrors('password')} ? 'has-error'">
    <label class="form-label" for="password">Mật khẩu <span class="req" aria-hidden="true">*</span></label>
    <input type="password" id="password" th:field="*{password}" class="form-control"
           minlength="8" maxlength="72" required autocomplete="new-password"
           data-minlength="8" data-pattern="^(?=.*[A-Za-z])(?=.*\d).+$"
           data-msg-required="Vui lòng nhập mật khẩu."
           data-msg-minlength="Mật khẩu cần từ 8 đến 72 ký tự."
           data-msg-pattern="Mật khẩu phải có cả chữ cái và chữ số."
           th:attr="aria-invalid=${#fields.hasErrors('password')},aria-describedby='password-hint password-error'">
    <p class="form-hint" id="password-hint">Tối thiểu 8 ký tự, gồm cả chữ cái và chữ số.</p>
    <p class="field-error" id="password-error" role="alert"
       th:if="${#fields.hasErrors('password')}" th:text="${#fields.errors('password')[0]}"></p>
  </div>

  <div class="form-group" th:classappend="${#fields.hasErrors('confirmPassword')} ? 'has-error'">
    <label class="form-label" for="confirmPassword">Xác nhận mật khẩu <span class="req" aria-hidden="true">*</span></label>
    <input type="password" id="confirmPassword" th:field="*{confirmPassword}" class="form-control"
           required autocomplete="new-password" data-match="password"
           data-msg-required="Vui lòng nhập lại mật khẩu."
           data-msg-match="Mật khẩu xác nhận không khớp."
           th:attr="aria-invalid=${#fields.hasErrors('confirmPassword')},aria-describedby='confirmPassword-error'">
    <p class="field-error" id="confirmPassword-error" role="alert"
       th:if="${#fields.hasErrors('confirmPassword')}" th:text="${#fields.errors('confirmPassword')[0]}"></p>
  </div>

  <div class="form-actions">
    <button type="submit" class="btn btn-primary">Đăng ký</button>
  </div>
</form>
```

Quy tắc bắt buộc cho mọi trường:
- Có `id` duy nhất; thông báo lỗi có `id="{id}-error"` (JS dựa vào quy ước này).
- Có `aria-invalid` và `aria-describedby`; thông báo lỗi có `role="alert"`.
- Có dấu `*` cho trường bắt buộc và `autocomplete` phù hợp.
- Thông báo ở HTML (`data-msg-*`) phải trùng với `messages.properties`.

### 4.2 CSS

```css
.field-error {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.4;
  color: var(--danger, #dc2626);
  overflow-wrap: anywhere;
}
.field-error[hidden] { display: none; }

.form-hint {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--text-muted, #64748b);
}

.has-error .form-control,
.has-error .form-select,
[aria-invalid="true"] {
  border-color: var(--danger, #dc2626);
  box-shadow: 0 0 0 3px rgba(220, 38, 38, .12);
}

.form-alert {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #991b1b;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.req { color: var(--danger, #dc2626); }
```

### 4.3 JavaScript (`static/js/form-validation.js`)

Nạp một lần trong layout chung (`layout-base.html`) bằng `<script th:src="@{/js/form-validation.js}" defer></script>`. Không dùng thư viện ngoài. Chính sách CSP `script-src 'self'` vẫn hợp lệ.

```js
/**
 * Validate form phía client. Kích hoạt cho form có thuộc tính data-validate.
 * Quy ước: thông báo lỗi nằm ở phần tử có id "<id-trường>-error".
 */
(function () {
    'use strict';

    var DEFAULT_REQUIRED = 'Vui lòng điền thông tin này.';

    function getFields(form) {
        return Array.prototype.slice.call(form.querySelectorAll('input, select, textarea'))
            .filter(function (f) {
                return f.type !== 'hidden' && f.type !== 'submit' && f.type !== 'button' && !f.disabled;
            });
    }

    function errorBox(field) {
        var id = field.id + '-error';
        var box = document.getElementById(id);
        if (!box) {
            box = document.createElement('p');
            box.id = id;
            box.className = 'field-error';
            box.setAttribute('role', 'alert');
            box.hidden = true;
            var group = field.closest('.form-group') || field.parentNode;
            group.appendChild(box);
        }
        return box;
    }

    /** Trả về thông báo lỗi, hoặc chuỗi rỗng nếu hợp lệ. */
    function check(form, field) {
        var d = field.dataset;
        var isPassword = field.type === 'password';
        var val = isPassword ? field.value : String(field.value || '').trim();

        if (field.type === 'checkbox') {
            if (field.required && !field.checked) return d.msgRequired || DEFAULT_REQUIRED;
            return '';
        }
        if (field.required && val === '') return d.msgRequired || DEFAULT_REQUIRED;
        if (val === '') return '';

        if (d.minlength && val.length < Number(d.minlength)) {
            return d.msgMinlength || ('Cần ít nhất ' + d.minlength + ' ký tự.');
        }
        if (d.maxlength && val.length > Number(d.maxlength)) {
            return d.msgMaxlength || ('Không quá ' + d.maxlength + ' ký tự.');
        }
        if (d.pattern) {
            var re;
            try { re = new RegExp(d.pattern, 'u'); } catch (e) { re = null; }
            if (re && !re.test(val)) return d.msgPattern || 'Giá trị không hợp lệ.';
        }
        if (d.match) {
            var other = form.elements[d.match];
            if (other && other.value !== field.value) return d.msgMatch || 'Giá trị không khớp.';
        }
        return '';
    }

    function show(field, msg) {
        var box = errorBox(field);
        var group = field.closest('.form-group');
        if (msg) {
            box.textContent = msg;
            box.hidden = false;
            field.setAttribute('aria-invalid', 'true');
            if (group) group.classList.add('has-error');
        } else {
            box.textContent = '';
            box.hidden = true;
            field.setAttribute('aria-invalid', 'false');
            if (group) group.classList.remove('has-error');
        }
    }

    function init(form) {
        form.noValidate = true;
        var fields = getFields(form);
        var touched = {};

        function validate(field) {
            var msg = check(form, field);
            show(field, msg);
            return msg;
        }

        fields.forEach(function (f) {
            f.addEventListener('blur', function () {
                touched[f.id] = true;
                validate(f);
            });
            var live = function () {
                if (touched[f.id] || f.getAttribute('aria-invalid') === 'true') validate(f);
                // Đổi mật khẩu thì kiểm tra lại ô xác nhận đã chạm vào
                fields.forEach(function (o) {
                    if (o.dataset.match === f.name && (touched[o.id] || o.getAttribute('aria-invalid') === 'true')) {
                        validate(o);
                    }
                });
            };
            f.addEventListener('input', live);
            f.addEventListener('change', live);
        });

        form.addEventListener('submit', function (e) {
            var firstBad = null;
            fields.forEach(function (f) {
                touched[f.id] = true;
                if (validate(f) && !firstBad) firstBad = f;
            });
            if (firstBad) {
                e.preventDefault();
                firstBad.focus();
            }
        });

        // Lỗi do server trả về: focus vào trường lỗi đầu tiên
        var serverBad = form.querySelector('[aria-invalid="true"]');
        if (serverBad) serverBad.focus();
    }

    function boot() {
        document.querySelectorAll('form[data-validate]').forEach(init);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
```

Ghi chú: JS dùng `data-pattern` (RegExp với cờ `u` để hỗ trợ `\p{L}`) thay vì thuộc tính `pattern` gốc của HTML, vì thuộc tính gốc phụ thuộc phiên bản trình duyệt. Không đặt thuộc tính `pattern` cho các trường dùng `\p{L}`.

## 5. Quy ước theo từng loại trường

| Trường | Quy tắc (server và client giống nhau) |
|---|---|
| Họ, Tên đệm và tên | Bắt buộc, chữ cái Unicode, khoảng trắng, `.'’-`, tối đa 50 / 80 ký tự |
| Email | Bắt buộc, regex `EMAIL_REGEX`, tối đa 150 ký tự, chuyển về chữ thường khi lưu |
| Mật khẩu | Bắt buộc, 8 đến 72 ký tự, có cả chữ cái và chữ số (72 là giới hạn của BCrypt) |
| Xác nhận mật khẩu | Bắt buộc, khớp mật khẩu |
| Ô chọn (môn học, phân hiệu, vai trò) | `@NotNull` với `Long`; option đầu có `value=""` |
| Mã OTP | Chỉ số, đúng độ dài cấu hình, không `autocomplete` gợi ý lại |
| Lý do khóa tài khoản | Bắt buộc, 5 đến 500 ký tự |
| Email xác nhận khi xóa | Bắt buộc, phải trùng email của tài khoản cần xóa |
| Tệp tải lên | Bắt buộc, đuôi pdf/doc/docx, tối đa 10 MB (client kiểm tra trước, server kiểm tra lại cả đuôi và content-type) |
| Nhận xét, đánh giá | Bắt buộc, tối đa độ dài cột trong DB, hiển thị bộ đếm ký tự |

## 6. Cấm (anti-pattern)

- Chỉ validate ở client, hoặc chỉ ở server mà không hiển thị theo trường.
- Dùng `@RequestParam` rời rạc cho form nhiều trường.
- `redirect:` khi có lỗi mà không truyền lại dữ liệu và `BindingResult`.
- Viết cứng thông báo trong annotation hoặc rải rác trong controller.
- Trả về thông báo tiếng Anh mặc định của Hibernate Validator (`must not be blank`).
- Thông báo mơ hồ: "Dữ liệu không hợp lệ", "Có lỗi xảy ra".
- Thông báo đăng nhập tiết lộ email có tồn tại hay không.
- Điền lại mật khẩu vào ô nhập (`value`) sau khi lỗi.
- Dùng `th:utext` để hiển thị lỗi.
- Xóa thông báo lỗi bằng `alert()` hoặc tự ẩn sau vài giây.

## 7. Checklist trước khi hoàn thành

Với MỖI form được tạo hoặc sửa:

- [ ] Có DTO với `@Valid` + `BindingResult`; không còn `@RequestParam` rời rạc.
- [ ] Mọi thông báo nằm trong `messages.properties`, tiếng Việt, đúng quy ước khoá.
- [ ] Để trống từng trường bắt buộc: thấy lỗi dưới đúng trường (cả client và khi tắt JS).
- [ ] Nhập sai định dạng từng trường: thấy lỗi cụ thể, rõ cách sửa.
- [ ] Gửi dữ liệu sai bằng công cụ khác (tắt JS, hoặc dùng Postman): server vẫn từ chối và trả lỗi đúng trường.
- [ ] Lỗi nghiệp vụ (email trùng, OTP sai...) hiện ở đúng trường hoặc banner chung có `role="alert"`.
- [ ] Sau khi server trả lỗi: dữ liệu cũ còn nguyên (trừ mật khẩu), danh sách dropdown vẫn hiển thị.
- [ ] Trường lỗi đầu tiên được focus.
- [ ] Màn hình 320px: thông báo lỗi không làm vỡ layout.
- [ ] Không có thông báo lộ thông tin nhạy cảm; không log mật khẩu.

## 8. Khi gen code, AI phải

1. Đọc file này và `UI-RESPONSIVE-RULES.md` trước khi viết.
2. Với mỗi form: tạo DTO, cập nhật controller, thêm khoá vào `messages.properties`, sửa template theo mẫu mục 4.1.
3. Trả về TOÀN BỘ nội dung từng file đã sửa (không đưa patch rời rạc), kèm đường dẫn đầy đủ.
4. Cuối câu trả lời liệt kê các trường đã validate (quy tắc và thông báo) dưới dạng bảng, và ghi rõ đã tự kiểm tra theo checklist mục 7 hay còn điểm chưa chắc.