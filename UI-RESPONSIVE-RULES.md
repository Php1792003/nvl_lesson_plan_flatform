# UI RESPONSIVE RULES (BẮT BUỘC KHI GEN / SỬA GIAO DIỆN)

Áp dụng cho mọi template Thymeleaf, CSS, JS trong dự án. Mọi trang mới hoặc sửa phải đạt các rule dưới đây TRƯỚC KHI báo hoàn thành.

## 1. Nguyên tắc nền

1. **Mobile-first**: viết CSS mặc định cho màn nhỏ, dùng `min-width` media query để mở rộng.
2. **Không bao giờ để nội dung tràn ngang** (không có horizontal scroll ở `body` ở bất kỳ độ rộng nào từ 320px).
3. **Không dùng width/height cố định** (px) cho container, card, button, input. Dùng `%`, `rem`, `flex`, `grid`, `max-width`, `clamp()`.
4. **Mọi flex/grid con chứa text** phải có `min-width: 0` để co lại được.
5. **Mọi hàng chứa >= 2 phần tử cạnh nhau** (avatar + info + badge + button...) phải có `flex-wrap: wrap` hoặc đổi sang cột ở mobile.
6. **Text dài** (email, tên, URL, mã) phải có `overflow-wrap: anywhere` hoặc `text-overflow: ellipsis` (kèm `white-space: nowrap; overflow: hidden` và `title`).
7. **Badge/tag/trạng thái**: `white-space: nowrap`, không bao giờ để chữ bị ngắt thành nhiều dòng như "Đang / hoạt / động".

## 2. Breakpoint chuẩn

| Tên | Rộng | Thiết bị tham chiếu |
|-----|------|---------------------|
| xs (mặc định) | < 480px | iPhone SE 320/375, iPhone 12 390, Android nhỏ |
| sm | >= 480px | điện thoại ngang |
| md | >= 768px | tablet dọc (iPad) |
| lg | >= 1024px | tablet ngang, laptop nhỏ |
| xl | >= 1280px | desktop |

Chỉ dùng các mốc trên, không tự nghĩ thêm mốc lẻ.

## 3. Viewport & nền tảng

- Mọi layout phải có: `<meta name="viewport" content="width=device-width, initial-scale=1">`
- `*, *::before, *::after { box-sizing: border-box; }`
- `img, svg, video, canvas { max-width: 100%; height: auto; }`
- Font input >= 16px trên mobile (tránh iOS tự zoom khi focus).
- Chiều cao full màn hình dùng `min-height: 100dvh`, không dùng `100vh` đơn thuần.

## 4. Nút bấm (Button)

### 4.1 Phân cấp
- Mỗi màn hình / mỗi khối chỉ có **tối đa 1 nút primary**.
- Nút phụ: `secondary` hoặc `outline/ghost`.
- Nút nguy hiểm (khóa, xóa, hủy kích hoạt): `danger`, luôn có bước xác nhận (modal hoặc `confirm`), tách khỏi nhóm nút thường.

### 4.2 Nhãn ngắn gọn
- Nhãn nút tối đa 1-3 từ. Ngữ cảnh đã rõ thì bỏ danh từ lặp.

| Thay vì | Dùng |
|---------|------|
| Khóa tài khoản | Khóa |
| Quay lại danh sách | Quay lại |
| Lưu thay đổi | Lưu |
| Thêm giáo viên mới | Thêm mới |
| Xuất file Excel | Xuất Excel |

- Nút trên thanh công cụ (toolbar) ở mobile: **chỉ icon**, có `aria-label` và `title`. Từ `md` trở lên hiện thêm chữ.

### 4.3 Số lượng
- Hàng action có >= 3 nút: giữ 1 nút chính + gom phần còn lại vào menu dropdown "⋯" (More).
- Trong bảng/card dòng dữ liệu: tối đa 2 icon button, còn lại vào menu "⋯".

### 4.4 Kích thước & vùng chạm
- Chiều cao tối thiểu **40px** (khuyến nghị 44px) trên mobile, vùng chạm >= 44x44px cho icon button.
- Khoảng cách giữa 2 nút >= 8px.
- Nút không có `width` cố định. Dùng `padding` + `white-space: nowrap`.

### 4.5 Nhóm nút cuối form (form actions)
- Desktop: canh phải, nằm ngang, `gap: 8px`.
- Mobile (< 480px): xếp dọc, full width, nút primary nằm TRÊN, nút phụ nằm dưới (`flex-direction: column-reverse` nếu HTML để phụ trước).
- Form dài: cho phép sticky action bar ở đáy màn hình mobile.

## 5. Pattern chuẩn (copy dùng lại)

### 5.1 Profile / Summary card (avatar + thông tin + trạng thái + hành động)

```html
<div class="profile-card">
  <div class="profile-card__main">
    <div class="avatar" th:text="${initial}">H</div>
    <div class="profile-card__info">
      <h2 class="profile-card__name" th:text="${name}">Hoàng Phúc</h2>
      <p class="profile-card__email" th:text="${email}">email@example.com</p>
    </div>
  </div>
  <div class="profile-card__actions">
    <span class="badge badge--success">Đang hoạt động</span>
    <button type="button" class="btn btn--danger btn--sm">Khóa</button>
  </div>
</div>
```

```css
.profile-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
}
.profile-card__main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 240px;
  min-width: 0;
}
.avatar { flex: 0 0 48px; width: 48px; height: 48px; border-radius: 50%; }
.profile-card__info { min-width: 0; }
.profile-card__name { margin: 0; overflow-wrap: anywhere; }
.profile-card__email { margin: 0; overflow-wrap: anywhere; }
.profile-card__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.badge { white-space: nowrap; }

@media (max-width: 479.98px) {
  .profile-card__actions { width: 100%; }
  .profile-card__actions .btn { flex: 1; }
}
```

### 5.2 Form actions

```html
<div class="form-actions">
  <a th:href="@{/...}" class="btn btn--secondary">Quay lại</a>
  <button type="submit" class="btn btn--primary">Lưu</button>
</div>
```

```css
.form-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
@media (max-width: 479.98px) {
  .form-actions { flex-direction: column-reverse; }
  .form-actions .btn { width: 100%; }
}
```

### 5.3 Lưới form

```css
.form-grid { display: grid; grid-template-columns: 1fr; gap: 16px; }
@media (min-width: 768px) { .form-grid { grid-template-columns: repeat(2, 1fr); } }
.form-grid > * { min-width: 0; }
.form-control { width: 100%; font-size: 16px; }
```

### 5.4 Bảng dữ liệu

- Từ `md` trở lên: dùng `<table>` bình thường, bọc trong `<div class="table-responsive" style="overflow-x:auto">`.
- Dưới `md`: chuyển mỗi dòng thành **card** (ẩn `thead`, mỗi `td` hiện `data-label` bằng `::before`) hoặc ẩn bớt cột phụ.
- Không bao giờ để bảng làm vỡ layout `body`.

### 5.5 Modal / Dialog

- Mobile: rộng `calc(100% - 32px)`, `max-height: 90dvh`, nội dung `overflow-y: auto`.
- Từ `md`: `max-width` theo nội dung (480 / 640 / 800px).
- Nút trong modal áp dụng rule 4.5.

### 5.6 Header / Sidebar

- Dưới `lg`: sidebar thành drawer (nút hamburger), có overlay, khóa scroll `body` khi mở.
- Tiêu đề header: `overflow: hidden; text-overflow: ellipsis; white-space: nowrap`.
- Icon header: tối đa 3-4 icon, còn lại gom vào menu.

## 6. Typography & khoảng cách

- Cỡ chữ cơ bản 14-16px, tiêu đề dùng `clamp()` (vd: `font-size: clamp(1.125rem, 2.5vw, 1.5rem)`).
- Padding card: 12-16px ở mobile, 20-24px từ `md`.
- Không dùng `px` cố định cho khoảng cách lớn, ưu tiên `rem` hoặc `gap`.

## 7. Cấm (anti-pattern)

- `width: 300px`, `height: 40px` cố định cho container/button/input.
- `white-space: nowrap` cho đoạn text dài mà không kèm ellipsis hoặc scroll.
- Hàng flex nhiều phần tử mà thiếu `flex-wrap` và `min-width: 0`.
- Nút có nhãn dài (> 3 từ) trên mobile.
- Dùng `position: absolute` để căn layout chính.
- `overflow-x: hidden` trên `body` để che lỗi tràn (phải sửa tận gốc).
- Hover-only interaction (tooltip, menu) mà không có cách dùng trên cảm ứng.
- Inline style cho layout (trừ trường hợp động bắt buộc).
- Chỉ test ở màn desktop.

## 8. Checklist trước khi hoàn thành (bắt buộc)

Test bằng Chrome DevTools (Device Toolbar) ở các độ rộng: **320, 375, 390, 414, 768, 1024, 1280**, cả dọc và ngang cho mobile:

- [ ] Không có thanh cuộn ngang ở `body`.
- [ ] Không có chữ bị ngắt xấu (badge, nút, tiêu đề).
- [ ] Không có phần tử bị cắt, bị tràn ra ngoài card.
- [ ] Mọi nút bấm được dễ dàng bằng ngón tay (>= 40-44px).
- [ ] Form actions đúng pattern 4.5 ở mobile.
- [ ] Bảng/danh sách dùng được ở mobile.
- [ ] Modal không vượt quá màn hình, cuộn được.
- [ ] Sidebar/menu mở, đóng được ở mobile.
- [ ] Zoom chữ 200% vẫn dùng được.
- [ ] Chạy lại các trang liên quan, không lỗi console.

## 9. Khi gen code mới, AI phải

1. Đọc file rule này trước.
2. Dùng lại class/pattern có sẵn trong CSS dự án (không tạo class trùng chức năng).
3. Trả về TOÀN BỘ nội dung file đã sửa (HTML + CSS), kèm đường dẫn đầy đủ.
4. Cuối câu trả lời, ghi 1 dòng xác nhận đã tự kiểm tra theo checklist mục 8 và nêu rõ nếu có điểm chưa chắc.
