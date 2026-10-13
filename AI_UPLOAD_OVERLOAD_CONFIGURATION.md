from pathlib import Path

content = """# YÊU CẦU AI CẤU HÌNH CHỐNG QUÁ TẢI UPLOAD

## 1. Bối cảnh hệ thống

- Máy chủ triển khai: laptop cá nhân chạy Linux.
- RAM: 4 GB.
- Ổ lưu trữ: SSD 500 GB, hiện còn trống khoảng 400 GB.
- Người dùng dự kiến: 250 giáo viên.
- Hành vi upload: rải rác trong ngày, có khả năng xuất hiện các đợt tăng tải.
- Dung lượng mỗi file dự kiến: 5–20 MB.
- Mục tiêu: giảm nguy cơ cạn RAM, nghẽn backend, đầy ổ đĩa tạm và mất dữ liệu khi nhiều người tải file lên.

## 2. Nhiệm vụ dành cho AI

Hãy kiểm tra source code và cấu trúc dự án hiện có trước khi chỉnh sửa. Tự xác định framework, ngôn ngữ, cơ chế xác thực, nơi lưu file, cơ sở dữ liệu, cách triển khai và cấu hình Nginx hiện tại.

Sau đó thiết kế và cấu hình giải pháp chống quá tải upload phù hợp với kiến trúc hiện tại. Không tự ý thay đổi công nghệ, cơ sở dữ liệu, API công khai hoặc quy trình nghiệp vụ nếu không thật sự cần thiết.

**Không chỉ đưa ra hướng dẫn hoặc ví dụ code trong câu trả lời. Hãy trực tiếp cập nhật các file cấu hình/source cần thiết trong dự án nếu môi trường làm việc cho phép, rồi báo cáo chính xác những gì đã thay đổi.**

## 3. Thiết kế cần triển khai

### 3.1. Rate limiting theo tài khoản

Ưu tiên thuật toán **Token Bucket** cho endpoint upload:

- Hạn mức khởi đầu: tốc độ bổ sung 1 token mỗi 10 giây cho mỗi tài khoản đã xác thực.
- Dung lượng bucket ban đầu: 3 token.
- Đây là thông số thử nghiệm ban đầu, không phải giới hạn đã được chứng minh là tối ưu.
- Mỗi yêu cầu upload tiêu thụ token theo quy tắc nhất quán.
- Lấy danh tính từ session/token đã xác thực ở phía server; không tin user ID hoặc username do client tự khai báo.
- Nếu vượt hạn mức, trả phản hồi phù hợp, ưu tiên HTTP 429 và `Retry-After` khi xác định được thời gian chờ.
- Không dùng giới hạn theo IP làm cơ chế duy nhất theo tài khoản, vì nhiều giáo viên có thể dùng chung IP.
- Nếu ứng dụng có nhiều instance hoặc nhiều tiến trình cần chia sẻ hạn mức, dùng cơ chế lưu trạng thái dùng chung phù hợp. Nếu chỉ có một instance, có thể dùng trạng thái trong bộ nhớ nhưng phải ghi rõ giới hạn này.

### 3.2. Giới hạn upload đồng thời

- Mức khởi đầu: tối đa 3 upload đang được tiếp nhận đồng thời trên toàn bộ instance ứng dụng.
- Khi không còn slot, từ chối hoặc yêu cầu thử lại có kiểm soát; không tạo số lượng kết nối chờ vô hạn trong backend.
- Mọi slot phải được giải phóng an toàn khi upload thành công, lỗi, timeout hoặc client ngắt kết nối.
- Không giữ slot tiếp nhận upload trong suốt thời gian xử lý nền sau khi file đã được lưu bền vững.
- Nếu có nhiều instance, bảo đảm giới hạn tổng thể được thực thi dùng chung thay vì áp dụng 3 slot riêng cho từng instance.

### 3.3. Hàng đợi xử lý file

Nếu hệ thống có công việc nặng sau upload (quét file, trích xuất nội dung, tạo preview, chuyển đổi định dạng hoặc tác vụ tương tự):

- Khởi đầu với 1 worker xử lý nền.
- Giới hạn hàng đợi ở mức 20 tác vụ chờ.
- Khi hàng đợi đầy, từ chối hoặc hoãn tiếp nhận công việc mới có kiểm soát; không làm mất file đã nhận và không báo xử lý thành công sai sự thật.
- Ưu tiên hàng đợi bền vững nếu dự án đã có cơ sở dữ liệu hoặc hệ thống queue phù hợp.
- Không thêm Redis, RabbitMQ hoặc dịch vụ mới một cách máy móc nếu chưa đánh giá nhu cầu, tài nguyên và hạ tầng hiện có.
- Nếu không có xử lý nền nặng, không tạo thêm worker/queue không cần thiết; giải thích rõ quyết định này.

### 3.4. Giới hạn file và lưu trữ

- Giới hạn kích thước mặc định ban đầu: 20 MB mỗi file, trừ khi nghiệp vụ hiện tại có yêu cầu khác đã được xác nhận.
- Kiểm tra kích thước ở cả reverse proxy và backend.
- Kiểm tra loại file, quyền người dùng, tên file và đường dẫn lưu; không tin hoàn toàn vào MIME type do client gửi.
- Không nạp toàn bộ file 5–20 MB vào RAM nếu có thể stream hoặc ghi file theo từng phần.
- Dùng tên lưu trữ an toàn do server tạo; tránh path traversal và ghi đè file ngoài ý muốn.
- Dùng thư mục tạm riêng, có quyền truy cập tối thiểu và quy trình dọn file tạm an toàn.
- Đảm bảo file chỉ được coi là đã upload thành công sau khi lưu hoàn tất và trạng thái dữ liệu cần thiết đã được ghi nhận.
- Kiểm tra dung lượng đĩa trước khi xử lý; tránh để file tạm hoặc file lỗi làm đầy ổ đĩa.
- Không tự xóa file người dùng hợp lệ để giải phóng dung lượng nếu chưa có chính sách lưu trữ được xác nhận.

## 4. Nginx / reverse proxy

Nếu dự án dùng Nginx:

- Chỉ cấu hình giới hạn cho endpoint upload thực tế; không vô tình làm ảnh hưởng endpoint khác.
- Giới hạn body phù hợp với mức 20 MB và định dạng request thực tế (multipart/form-data có thể có overhead nhỏ).
- Cân nhắc `client_body_temp_path` và cơ chế buffer request để giảm áp lực RAM lên backend.
- Thiết lập timeout hợp lý cho việc nhận body, tránh ngắt các upload hợp lệ trên mạng chậm; không đặt timeout quá lớn mà không có lý do.
- Có thể dùng `limit_req` và `limit_conn` làm lớp bảo vệ bổ sung, nhưng không coi chúng là thay thế cho giới hạn theo tài khoản trong backend.
- Cẩn trọng với IP phía client: chỉ tin forwarded headers từ proxy đáng tin cậy; không cho phép giả mạo `X-Forwarded-For`.
- Nếu Nginx nằm sau proxy khác, xác minh cấu hình real IP trước khi dùng giới hạn theo IP.
- Giữ nguyên HTTPS, domain, route và cấu hình triển khai đang hoạt động.
- Chạy kiểm tra cú pháp cấu hình Nginx trước khi áp dụng hoặc reload.
- Không giả định AI có quyền truy cập máy chủ production; nếu chỉ có source code, hãy cập nhật cấu hình trong repository và nêu rõ bước triển khai thủ công còn lại.

## 5. Nguyên tắc an toàn và tương thích

- Trước khi sửa, đọc cấu trúc dự án, file README, file cấu hình, các endpoint upload và cơ chế lưu file hiện có.
- Không ghi đè hoặc xóa cấu hình hiện hữu nếu chưa hiểu mục đích.
- Không để lộ secret, mật khẩu, token hoặc thông tin nhạy cảm trong log.
- Tránh log nội dung file hoặc dữ liệu cá nhân không cần thiết.
- Dùng HTTP 413 khi file vượt kích thước tối đa; HTTP 429 khi vượt rate limit; dùng 503 khi hệ thống tạm thời không thể tiếp nhận công việc và có thể thử lại.
- Tránh trả lỗi 200 khi file hoặc metadata chưa được lưu thành công.
- Giữ API tương thích với frontend hiện tại; nếu phải thay đổi contract, nêu rõ lý do và cập nhật các bên liên quan.
- Không cài thêm package hoặc dịch vụ nếu không cần thiết; nếu phải thêm, giải thích lợi ích, chi phí tài nguyên và cách cấu hình.

## 6. Kiểm thử và tiêu chí nghiệm thu

Sau khi chỉnh sửa, thực hiện những kiểm tra có thể chạy trong môi trường hiện tại:

1. Kiểm tra build/test hiện có và không làm hỏng chức năng hiện tại.
2. File hợp lệ trong giới hạn có thể upload và được lưu đúng.
3. File vượt giới hạn bị từ chối với mã lỗi phù hợp.
4. Người dùng chưa xác thực hoặc không có quyền không thể upload.
5. Vượt Token Bucket nhận được HTTP 429 và thông tin thử lại phù hợp khi có thể.
6. Khi hết slot upload, hệ thống từ chối/hoãn có kiểm soát; slot được giải phóng sau lỗi hoặc ngắt kết nối.
7. Hàng đợi không vượt giới hạn đã cấu hình; lỗi xử lý không làm mất trạng thái công việc.
8. Không đọc toàn bộ file vào RAM nếu kiến trúc hỗ trợ streaming.
9. Kiểm tra đường dẫn lưu, tên file, file tạm và quyền truy cập.
10. Kiểm tra cấu hình Nginx bằng công cụ phù hợp nếu Nginx có trong môi trường.

Không tuyên bố đã thử tải 250 người đồng thời nếu chưa thực sự chạy bài kiểm thử đó. Nếu không thể chạy benchmark, ghi rõ đây là cấu hình ban đầu cần được thử tải trong môi trường staging.

## 7. Theo dõi và điều chỉnh

Ghi nhận tối thiểu các chỉ số không nhạy cảm:

- Số upload đang chạy.
- Số yêu cầu bị giới hạn (429), số lỗi (4xx/5xx) và timeout.
- Độ dài hàng đợi và tuổi của tác vụ chờ lâu nhất.
- Thời gian upload và thời gian xử lý nền.
- Mức sử dụng RAM, CPU, swap, dung lượng ổ đĩa và dung lượng thư mục tạm.

Dùng các chỉ số thực tế để điều chỉnh dần giới hạn 3 upload đồng thời và 1 worker. Không tăng đồng thời mọi giới hạn khi chưa xác định nút thắt tài nguyên.

## 8. Yêu cầu đầu ra của AI

Khi hoàn tất, báo cáo ngắn gọn:

1. Công nghệ và kiến trúc upload hiện tại đã phát hiện.
2. Các file đã tạo/sửa và mục đích của từng file.
3. Cách Token Bucket, giới hạn đồng thời và hàng đợi được triển khai.
4. Những giả định, giới hạn hoặc phần chưa thể tự động cấu hình.
5. Các lệnh cần chạy để kiểm tra/build/deploy.
6. Kết quả kiểm thử thực tế; phân biệt rõ kiểm thử đã chạy với kiểm thử còn cần thực hiện.
7. Các rủi ro còn lại khi chạy trên laptop RAM 4GB và khuyến nghị nâng cấp nếu kết quả tải thực tế không đạt.

Ưu tiên giải pháp đơn giản, dễ bảo trì, an toàn và tương thích với source hiện có. Không tự ý triển khai lên production hoặc thay đổi hệ thống đang chạy nếu chưa được cho phép.
"""

path = Path("AI_UPLOAD_OVERLOAD_CONFIGURATION.md")
path.write_text(content, encoding="utf-8")
print(f"Đã tạo file: {path}")
