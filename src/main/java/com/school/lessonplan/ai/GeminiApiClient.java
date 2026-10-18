package com.school.lessonplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.ai.dto.AiReviewResponse;
import com.school.lessonplan.config.AppProperties;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiApiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiClient.class);
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiApiClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean isConfigured() {
        String key = appProperties.getAi().getGemini().getApiKey();
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("none");
    }

    public AiReviewResponse reviewPlan(LessonPlan plan, LessonPlanVersion version, String extractedContent) {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API Key is not configured.");
        }

        String apiKey = appProperties.getAi().getGemini().getApiKey().trim();
        String model = appProperties.getAi().getGemini().getModel();
        if (model == null || model.isBlank()) {
            model = "gemini-1.5-flash";
        }
        String apiUrl = appProperties.getAi().getGemini().getApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = " ";
        }

        String endpoint = apiUrl + "/" + model + ":generateContent?key=" + apiKey;

        String subjectName = (plan.getDepartment() != null) ? plan.getDepartment().getName() : "Bộ môn";
        String teacherName = (plan.getOwner() != null) ? plan.getOwner().getFullName() : "Giáo viên";

        String prompt = """
                Bạn là Trưởng đoàn Thanh tra Chuyên môn / Phó Hiệu trưởng phụ trách chuyên môn của Ban Giám Hiệu trường THCS tại Việt Nam.
                Nhiệm vụ: Thẩm định KỸ THUẬT SOẠN GIÁO ÁN (Kế hoạch bài dạy) căn cứ theo 2 văn bản quy phạm pháp luật mới nhất của Bộ Giáo dục và Đào tạo:
                1. THÔNG TƯ 15/2026/TT-BGDĐT (chính thức có hiệu lực từ 10/5/2026: Quy định quản lý hồ sơ chuyên môn giáo viên - tinh giản thủ tục hành chính, đề cao tính linh hoạt, thực chất, tránh rập khuôn hình thức và đẩy mạnh ứng dụng học liệu số/chuyển đổi số).
                2. THÔNG TƯ 17/2025/TT-BGDĐT (Quy định điều chỉnh Chương trình Giáo dục phổ thông 2018: cập nhật Yêu cầu cần đạt, chú trọng phát triển Năng lực số, giáo dục STEM/STEAM, vận dụng thực tiễn và đổi mới phương thức đánh giá thường xuyên).
                (Kế thừa khung cấu trúc 4 hoạt động học và 4 bước tổ chức sư phạm chuẩn mực của CV 5512 nhưng thẩm định trên tinh thần thực chất, linh hoạt, hiện đại của TT 15/2026 & TT 17/2025).

                QUY TẮC BẮT BUỘC CHO THANH TRA BGH:
                1. TUYỆT ĐỐI KHÔNG nhận xét chung chung, sáo rỗng hay an ủi ngoại giao.
                2. PHẢI CHỈ ĐÍCH DANH CÁC LỖI KỸ THUẬT CỤ THỂ theo 7 tiêu chí mới để Ban Giám Hiệu dùng làm căn cứ trực tiếp kiểm tra, dự giờ giáo viên:
                   - Tiêu chí 1 (Mục tiêu - TT 17/2025): Bám sát YCCĐ điều chỉnh mới nhất; phân tách rõ Kiến thức cốt lõi, Năng lực chung, Năng lực đặc thù và Phẩm chất; có chú trọng Năng lực số / tích hợp STEM / giải quyết vấn đề thực tiễn hay chưa; động từ chỉ hành vi phải đo lường được.
                   - Tiêu chí 2 (Học liệu số & Thiết bị - TT 15/2026): Có ứng dụng học liệu số, phần mềm, thiết bị CNTT, đồ dùng trực quan phù hợp điều kiện thực tế; phân định rõ nhiệm vụ chuẩn bị của GV và HS.
                   - Tiêu chí 3 (Tiến trình dạy học thực chất - TT 15/2026): Chuỗi 4 hoạt động học (Khởi động -> Khám phá/Hình thành kiến thức -> Luyện tập -> Vận dụng) có thực chất, logic không? Có bị rườm rà hình thức không? (Lưu ý: HĐ Vận dụng phải gắn với thực tiễn, không thay thế bằng dặn bài tập về nhà).
                   - Tiêu chí 4 (Kỹ thuật 4 bước tổ chức): Trong từng hoạt động, có đủ 4 bước kỹ thuật chuẩn: 1. Chuyển giao NV -> 2. Thực hiện NV -> 3. Báo cáo, thảo luận -> 4. Kết luận, nhận định; học sinh chủ động thực hiện, GV hướng dẫn và chuẩn hóa kiến thức.
                   - Tiêu chí 5 (Sản phẩm học tập): Có mô tả rõ Sản phẩm học tập dự kiến đo lường được của HS (câu trả lời, phiếu học tập số hóa, bài tập giải xong, sản phẩm thực hành) hay ghi chung chung mơ hồ?
                   - Tiêu chí 6 (Phân bổ thời lượng khả thi - TT 15/2026): Dự kiến số phút cụ thể cho từng hoạt động phù hợp tổng thời lượng tiết dạy 45 phút, tránh quá tải đối với học sinh.
                   - Tiêu chí 7 (Đổi mới đánh giá thường xuyên - TT 17/2025): Có tích hợp công cụ đánh giá quá trình (rubric tiêu chí, bảng kiểm, câu hỏi nhanh, tự đánh giá hoặc đánh giá đồng đẳng) hay không?
                3. PHÁT HIỆN TỆP KHÔNG HỢP LỆ: Nếu nội dung tệp tải lên KHÔNG PHẢI LÀ GIÁO ÁN (ví dụ: tệp rỗng, đề kiểm tra, slide, biên bản, danh sách học sinh, tài liệu cá nhân hoặc văn bản linh tinh):
                   - BẮT BUỘC đặt overallRating là "Không đạt yêu cầu (Tệp không phải giáo án)".
                   - strengths để mảng rỗng [].
                   - improvements chỉ rõ: tệp không có cấu trúc Kế hoạch bài dạy theo Thông tư 15/2026 & Thông tư 17/2025, yêu cầu BGH kiểm tra việc giáo viên tải nhầm tệp.
                   - suggestedFeedback là văn bản BGH tuyên bố KHÔNG DUYỆT và yêu cầu nộp lại.

                THÔNG TIN BÀI DẠY:
                - Tên giáo án: %s
                - Môn học: %s | Khối: %s
                - Năm học: %s (Học kỳ %d)
                - Giáo viên soạn: %s
                - Phiên bản: v%d

                NỘI DUNG VĂN BẢN TRÍCH XUẤT TỪ TỆP:
                %s

                YÊU CẦU ĐẦU RA:
                Hãy trả về DUY NHẤT một chuỗi JSON hợp lệ (không kèm markdown ```json bên ngoài) theo cấu trúc sau:
                {
                  "overallRating": "Không đạt yêu cầu (Tệp không phải giáo án) / Chưa đạt yêu cầu (Cần chỉnh sửa theo TT 15/2026 & TT 17/2025) / Đạt yêu cầu (Cần hoàn thiện một số mục) / Tốt (Đạt chuẩn TT 15/2026 & TT 17/2025) / Xuất sắc (Mẫu mực chuẩn GDPT 2018 điều chỉnh)",
                  "summary": "Tóm tắt ngắn 1-2 câu kết luận kỹ thuật của Ban Giám Hiệu theo Thông tư 15/2026 và Thông tư 17/2025",
                  "strengths": [
                    "Điểm đạt chuẩn 1 theo TT 17/2025 về mục tiêu năng lực/phẩm chất hoặc tích hợp năng lực số/thực tiễn",
                    "Điểm đạt chuẩn 2 theo TT 15/2026 về chuỗi hoạt động học thực chất, không hình thức, 4 bước tổ chức rõ ràng"
                  ],
                  "improvements": [
                    "❌ [TIÊU CHÍ KỸ THUẬT LỖI] Chỉ rõ lỗi kỹ thuật theo TT 15/2026 hoặc TT 17/2025 để BGH đến kiểm tra trực tiếp giáo viên",
                    "❌ [TIÊU CHÍ KỸ THUẬT LỖI] Gợi ý điều chỉnh cụ thể"
                  ],
                  "suggestedFeedback": "Toàn văn biên bản thông báo kết luận chuyên môn của Ban Giám Hiệu gửi Thầy/Cô căn cứ theo Thông tư 15/2026/TT-BGDĐT và Thông tư 17/2025/TT-BGDĐT (có kính gửi, liệt kê điểm đạt, danh mục lỗi kỹ thuật cần khắc phục và kết luận duyệt/không duyệt)."
                }
                """
                .formatted(
                        plan.getTitle(),
                        subjectName,
                        plan.getGrade(),
                        plan.getSchoolYear(),
                        plan.getSemester(),
                        teacherName,
                        version.getVersionNo(),
                        extractedContent);

        try {
            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 2048,
                            "responseMimeType", "application/json"));

            String requestBody = objectMapper.writeValueAsString(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(appProperties.getAi().getGemini().getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseGeminiResponse(response.body(), model);
            } else {
                log.warn("Gemini API error (Status: {}): {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API error status " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Failed to call Google Gemini API: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private AiReviewResponse parseGeminiResponse(String responseJson, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();

                // Clean potential markdown code blocks ```json ... ```
                String cleanJson = text.trim();
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.substring(7);
                }
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.substring(3);
                }
                if (cleanJson.endsWith("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                }
                cleanJson = cleanJson.trim();

                JsonNode resultJson = objectMapper.readTree(cleanJson);

                AiReviewResponse res = new AiReviewResponse();
                res.setSuccess(true);
                res.setSource("GEMINI_AI");
                res.setModelName("Google Gemini (" + model + ")");
                res.setOverallRating(
                        resultJson.path("overallRating").asText("Tốt (Đạt chuẩn TT 15/2026 & TT 17/2025)"));
                res.setSummary(resultJson.path("summary").asText(""));
                res.setSuggestedFeedback(resultJson.path("suggestedFeedback").asText(""));

                List<String> strengths = new ArrayList<>();
                JsonNode strNode = resultJson.path("strengths");
                if (strNode.isArray()) {
                    strNode.forEach(n -> strengths.add(n.asText()));
                }
                res.setStrengths(strengths);

                List<String> improvements = new ArrayList<>();
                JsonNode impNode = resultJson.path("improvements");
                if (impNode.isArray()) {
                    impNode.forEach(n -> improvements.add(n.asText()));
                }
                res.setImprovements(improvements);

                return res;
            }
        } catch (Exception e) {
            log.warn("Error parsing Gemini JSON output: {}", e.getMessage());
        }
        throw new RuntimeException("Could not parse Gemini JSON response");
    }
}
