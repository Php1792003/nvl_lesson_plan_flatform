// src/test/java/com/school/lessonplan/ai/AiReviewPreCheckTest.java
package com.school.lessonplan.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.ai.client.AiClient;
import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.AiReviewResponse;
import com.school.lessonplan.ai.dto.ExtractedPlanText;
import com.school.lessonplan.ai.dto.InputType;
import com.school.lessonplan.ai.dto.ReviewVerdict;
import com.school.lessonplan.department.Department;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import com.school.lessonplan.lessonplan.LessonPlanVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiReviewPreCheckTest {

    @Mock
    private LessonPlanRepository lessonPlanRepository;

    @Mock
    private LessonPlanVersionRepository versionRepository;

    @Mock
    private com.school.lessonplan.evaluation.EvaluationRepository evaluationRepository;

    @Mock
    private PlanTextExtractor planTextExtractor;

    @Mock
    private AiReviewPromptBuilder promptBuilder;

    @Mock
    private AiClient aiClient;

    private AiReviewProperties properties;
    private AiReviewResultParser resultParser;
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        properties = new AiReviewProperties();
        properties.setMinChars(100);
        properties.setSourceLabel("Bộ thẩm định Chuẩn Sư phạm");
        resultParser = new AiReviewResultParser(new ObjectMapper(), properties);

        aiReviewService = new AiReviewService(
                lessonPlanRepository,
                versionRepository,
                evaluationRepository,
                planTextExtractor,
                promptBuilder,
                aiClient,
                resultParser,
                properties
        );
    }

    @Test
    @DisplayName("Test 1: File rỗng hoặc ảnh quét (textLength < 100) -> trả FILE_TRONG từ pre-check, AI KHÔNG được gọi")
    void testPreCheckEmptyFileNoAiCall() {
        Long planId = 1L;
        LessonPlan plan = new LessonPlan();
        plan.setId(planId);
        plan.setTitle("Bài 1: Căn bậc hai");
        plan.setGrade("9");
        Department dept = new Department();
        dept.setName("Toán học");
        plan.setDepartment(dept);

        LessonPlanVersion version = new LessonPlanVersion();
        version.setId(10L);
        version.setOriginalFilename("scan_giao_an.pdf");
        version.setStoredFilename("uuid-scan.pdf");
        version.setSha256("dummyhash12345");

        when(lessonPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(versionRepository.findCurrentVersion(planId)).thenReturn(Optional.of(version));

        // Mock Extractor: trích được chỉ 25 ký tự (< minChars = 100)
        ExtractedPlanText mockExtracted = new ExtractedPlanText(
                "Trường THCS Nguyễn Văn Linh",
                25,
                1,
                "scan_giao_an.pdf",
                "pdf"
        );
        when(planTextExtractor.extract(version)).thenReturn(mockExtracted);

        // Execute service
        AiReviewResponse response = aiReviewService.reviewPlan(planId, null);

        // Assertions
        assertNotNull(response);
        assertEquals(InputType.FILE_TRONG, response.getInputType());
        assertEquals(ReviewVerdict.KHONG_DUYET, response.getVerdict());
        assertEquals("Không đọc được nội dung tệp", response.getBadgeText());
        assertTrue(response.getPassedPoints().isEmpty());
        assertTrue(response.getSummary().contains("Không trích xuất được văn bản"));
        assertTrue(response.getConclusionText().contains("KHÔNG DUYỆT"));

        // VERIFY: AI Client KHÔNG ĐƯỢC PHÉP ĐƯỢC GỌI
        verify(aiClient, never()).complete(any(), any());
    }
}
