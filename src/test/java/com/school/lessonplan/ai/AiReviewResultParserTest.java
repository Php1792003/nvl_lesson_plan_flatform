// src/test/java/com/school/lessonplan/ai/AiReviewResultParserTest.java
package com.school.lessonplan.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.AiReviewResponse;
import com.school.lessonplan.ai.dto.ChecklistLevel;
import com.school.lessonplan.ai.dto.InputType;
import com.school.lessonplan.ai.dto.ReviewVerdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiReviewResultParserTest {

    private AiReviewResultParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        AiReviewProperties properties = new AiReviewProperties();
        properties.setSourceLabel("Bộ thẩm định Chuẩn Sư phạm");
        parser = new AiReviewResultParser(objectMapper, properties);
    }

    @Test
    @DisplayName("Test 7: AI trả JSON bọc trong markdown ```json ... ``` thì parser vẫn đọc được")
    void testMarkdownWrappedJson() {
        String markdownJson = """
                ```json
                {
                  "input_type": "GIAO_AN_HOP_LE",
                  "verdict": "DUYET",
                  "badge_text": "Đạt chuẩn sư phạm",
                  "summary": "Kế hoạch bài dạy đầy đủ 4 hoạt động và mục tiêu rõ ràng.",
                  "passed_points": [
                    "Mục tiêu bài dạy xác định rõ ràng yêu cầu cần đạt về kiến thức và năng lực"
                  ],
                  "checklist": [
                    {
                      "level": "GOI_Y",
                      "issue": "Nên bổ sung thêm câu hỏi phân hóa cho học sinh khá giỏi",
                      "fix": "Thêm 1 câu hỏi vận dụng nâng cao ở hoạt động 4"
                    }
                  ],
                  "conclusion_text": "Kính gửi Thầy/Cô, kế hoạch bài dạy đạt yêu cầu."
                }
                ```
                """;

        AiReviewResponse response = parser.parseAndValidate(markdownJson);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(InputType.GIAO_AN_HOP_LE, response.getInputType());
        assertEquals(ReviewVerdict.DUYET, response.getVerdict());
        assertEquals("Đạt chuẩn sư phạm", response.getBadgeText());
        assertEquals(1, response.getPassedPoints().size());
        assertEquals(1, response.getChecklist().size());
        assertEquals(ChecklistLevel.GOI_Y, response.getChecklist().get(0).getLevel());
    }

    @Test
    @DisplayName("Test 2: File CV (SAI_LOAI_TAI_LIEU) -> verdict = KHONG_DUYET, passed_points rỗng, summary nêu rõ CV")
    void testCvInvalidDocumentParsing() {
        String cvResponseJson = """
                {
                  "input_type": "SAI_LOAI_TAI_LIEU",
                  "verdict": "KHONG_DUYET",
                  "badge_text": "Không đạt: tệp không phải giáo án",
                  "summary": "Tệp đính kèm là hồ sơ cá nhân (CV), không phải kế hoạch bài dạy.",
                  "passed_points": ["Điểm giả mạo không được phép xuất hiện"],
                  "checklist": [
                    {
                      "level": "LOI_NGHIEM_TRONG",
                      "issue": "Tệp tải lên là CV cá nhân, không có tiến trình sư phạm",
                      "fix": "Nộp lại kế hoạch bài dạy đúng môn học"
                    }
                  ],
                  "conclusion_text": "Kính gửi Thầy/Cô, Ban Giám Hiệu nhận thấy tệp tải lên là CV cá nhân. Đề nghị Thầy/Cô nộp lại đúng kế hoạch bài dạy."
                }
                """;

        AiReviewResponse response = parser.parseAndValidate(cvResponseJson);

        assertNotNull(response);
        assertEquals(InputType.SAI_LOAI_TAI_LIEU, response.getInputType());
        assertEquals(ReviewVerdict.KHONG_DUYET, response.getVerdict());
        assertTrue(response.getPassedPoints().isEmpty(), "passedPoints bắt buộc phải rỗng khi tệp là SAI_LOAI_TAI_LIEU");
        assertTrue(response.getSummary().contains("CV"));
        assertFalse(response.getSummary().contains("bị hỏng hoặc quét ảnh"));
    }

    @Test
    @DisplayName("Test 5 & 8: verdict = DUYET nhưng checklist có LOI_NGHIEM_TRONG -> Parser hạ xuống KHONG_DUYET")
    void testConsistencyRuleSevereErrorDowngrade() {
        String inconsistentJson = """
                {
                  "input_type": "GIAO_AN_HOP_LE",
                  "verdict": "DUYET",
                  "badge_text": "Đạt chuẩn",
                  "summary": "Bài dạy cơ bản ổn nhưng có lỗi nghiêm trọng.",
                  "passed_points": ["Cấu trúc 4 bước rõ ràng"],
                  "checklist": [
                    {
                      "level": "LOI_NGHIEM_TRONG",
                      "issue": "Công thức Toán học tại Hoạt động 2 bị sai định lý Pythagoras",
                      "fix": "Sửa lại a^2 = b^2 + c^2 thành a^2 + b^2 = c^2"
                    }
                  ],
                  "conclusion_text": "Kính gửi Thầy/Cô, cần sửa lỗi công thức."
                }
                """;

        AiReviewResponse response = parser.parseAndValidate(inconsistentJson);

        assertNotNull(response);
        assertEquals(ReviewVerdict.KHONG_DUYET, response.getVerdict(),
                "Khi có LOI_NGHIEM_TRONG thì verdict không được phép là DUYET");
        assertEquals(1, response.getChecklist().size());
        assertEquals(ChecklistLevel.LOI_NGHIEM_TRONG, response.getChecklist().get(0).getLevel());
    }

    @Test
    @DisplayName("Test: Parser trích xuất đúng trường bgh_follow_up")
    void testBghFollowUpFieldParsing() {
        String json = """
                {
                  "input_type": "GIAO_AN_HOP_LE",
                  "verdict": "DUYET",
                  "badge_text": "Đạt chuẩn",
                  "summary": "Giáo án chuẩn sư phạm.",
                  "bgh_follow_up": "Giáo viên đã tiếp thu ý kiến của BGH và bổ sung đầy đủ phiếu học tập.",
                  "passed_points": ["Đã tiếp thu góp ý của BGH"],
                  "checklist": [],
                  "conclusion_text": "Kính gửi Thầy/Cô..."
                }
                """;

        AiReviewResponse response = parser.parseAndValidate(json);
        assertNotNull(response);
        assertEquals("Giáo viên đã tiếp thu ý kiến của BGH và bổ sung đầy đủ phiếu học tập.", response.getBghFollowUp());
    }
}
