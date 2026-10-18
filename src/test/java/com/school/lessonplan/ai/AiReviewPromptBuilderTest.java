// src/test/java/com/school/lessonplan/ai/AiReviewPromptBuilderTest.java
package com.school.lessonplan.ai;

import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.ExtractedPlanText;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.lessonplan.LessonPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

class AiReviewPromptBuilderTest {

    @Test
    @DisplayName("Test 6: legal-basis.md rỗng -> kết quả căn cứ pháp lý là (Không có) và không chứa số hiệu thông tư bịa")
    void testLegalBasisEmpty() {
        AiReviewProperties properties = new AiReviewProperties();
        properties.setLegalBasisPath("classpath:legal-basis.md");

        AiReviewPromptBuilder builder = new AiReviewPromptBuilder(properties, new DefaultResourceLoader());

        String legalBasis = builder.loadLegalBasis();
        assertEquals("(Không có)", legalBasis);

        LessonPlan plan = new LessonPlan();
        plan.setTitle("Phương trình bậc hai");
        plan.setGrade("9");
        Department dept = new Department();
        dept.setName("Toán học");
        plan.setDepartment(dept);

        ExtractedPlanText extracted = new ExtractedPlanText(
                "Nội dung bài học phương trình bậc hai một ẩn...",
                50,
                1,
                "giaoan_toan9.docx",
                "docx"
        );

        String userMessage = builder.buildUserMessage(plan, extracted);

        assertTrue(userMessage.contains("[CĂN CỨ_PHÁP_LÝ]\n(Không có)"));
        assertFalse(userMessage.contains("TT 15/2026"));
        assertFalse(userMessage.contains("TT 17/2025"));
    }

    @Test
    @DisplayName("Test BGH Study: buildUserMessage đưa đúng lịch sử nhận xét của BGH vào prompt để AI nghiên cứu")
    void testBghCommentsHistoryFormatting() {
        AiReviewProperties properties = new AiReviewProperties();
        AiReviewPromptBuilder builder = new AiReviewPromptBuilder(properties, new DefaultResourceLoader());

        LessonPlan plan = new LessonPlan();
        plan.setTitle("Bài 5: Định luật Ôm");
        plan.setGrade("9");
        Department dept = new Department();
        dept.setName("Vật lý");
        plan.setDepartment(dept);

        ExtractedPlanText extracted = new ExtractedPlanText("Nội dung bài dạy...", 100, 1, "vatly9.docx", "docx");

        com.school.lessonplan.lessonplan.LessonPlanVersion v1 = new com.school.lessonplan.lessonplan.LessonPlanVersion();
        v1.setVersionNo(1);

        com.school.lessonplan.evaluation.Evaluation eval1 = new com.school.lessonplan.evaluation.Evaluation();
        eval1.setVersion(v1);
        eval1.setReviewerNameSnapshot("Thầy Hiệu Trưởng");
        eval1.setContent("Cần bổ sung phiếu học tập và chia nhóm rõ ràng ở hoạt động Luyện tập.");
        eval1.setCreatedAt(java.time.Instant.parse("2026-10-10T07:30:00Z"));

        String userMessage = builder.buildUserMessage(plan, extracted, java.util.List.of(eval1), java.util.Collections.emptyList());

        assertTrue(userMessage.contains("[LỊCH SỬ_VÀ_PHONG_CÁCH_NHẬN_XÉT_CỦA_BAN_GIÁM_HIỆU]"));
        assertTrue(userMessage.contains("Thầy Hiệu Trưởng"));
        assertTrue(userMessage.contains("Cần bổ sung phiếu học tập và chia nhóm rõ ràng ở hoạt động Luyện tập."));
        assertTrue(userMessage.contains("Phiên bản v1"));
    }
}
