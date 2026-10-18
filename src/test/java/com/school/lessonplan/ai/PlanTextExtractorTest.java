// src/test/java/com/school/lessonplan/ai/PlanTextExtractorTest.java
package com.school.lessonplan.ai;

import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.ExtractedPlanText;
import com.school.lessonplan.ai.exception.FileUnreadableException;
import com.school.lessonplan.lessonplan.FileStorageService;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanTextExtractorTest {

    @Mock
    private FileStorageService fileStorageService;

    private AiReviewProperties properties;
    private PlanTextExtractor extractor;

    @BeforeEach
    void setUp() {
        properties = new AiReviewProperties();
        properties.setMaxChars(200); // Set small maxChars for easy truncation test
        extractor = new PlanTextExtractor(fileStorageService, properties);
    }

    @Test
    @DisplayName("Test 10: Văn bản vượt max-chars -> bị cắt và có dòng ghi chú gửi cho AI")
    void testTruncateWhenExceedsMaxChars() {
        LessonPlanVersion version = new LessonPlanVersion();
        version.setOriginalFilename("test_lesson.txt");
        version.setStoredFilename("stored_test.txt");

        // String of 300 characters
        String longText = "A".repeat(300);
        ByteArrayResource resource = new ByteArrayResource(longText.getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.loadFileAsResource("stored_test.txt")).thenReturn(resource);

        ExtractedPlanText result = extractor.extract(version);

        assertNotNull(result);
        assertEquals(300, result.getTextLength()); // textLength reflects normalized raw length
        assertTrue(result.getText().contains("[ĐÃ CẮT BỚT NỘI DUNG: còn 100 ký tự không gửi]"));
        assertTrue(result.getText().startsWith("A".repeat(200)));
    }

    @Test
    @DisplayName("Test file không đọc được hoặc không tìm thấy -> ném FileUnreadableException")
    void testUnreadableFileThrowsException() {
        LessonPlanVersion version = new LessonPlanVersion();
        version.setOriginalFilename("broken.pdf");
        version.setStoredFilename("not_found.pdf");

        when(fileStorageService.loadFileAsResource("not_found.pdf")).thenThrow(new RuntimeException("File not found on disk"));

        assertThrows(FileUnreadableException.class, () -> extractor.extract(version));
    }
}
