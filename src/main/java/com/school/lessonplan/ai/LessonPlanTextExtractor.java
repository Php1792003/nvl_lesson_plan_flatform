package com.school.lessonplan.ai;

import com.school.lessonplan.lessonplan.FileStorageService;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class LessonPlanTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(LessonPlanTextExtractor.class);
    private final FileStorageService fileStorageService;

    public LessonPlanTextExtractor(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String extractContent(LessonPlan plan, LessonPlanVersion version) {
        StringBuilder sb = new StringBuilder();

        // 1. Metadata context
        sb.append("Tên giáo án: ").append(plan.getTitle()).append("\n");
        if (plan.getDepartment() != null) {
            sb.append("Tổ bộ môn: ").append(plan.getDepartment().getName()).append("\n");
        }
        sb.append("Khối: ").append(plan.getGrade()).append("\n");
        sb.append("Năm học: ").append(plan.getSchoolYear()).append(" - Học kỳ: ").append(plan.getSemester()).append("\n");
        if (plan.getOwner() != null) {
            sb.append("Giáo viên soạn: ").append(plan.getOwner().getFullName()).append("\n");
        }
        sb.append("Phiên bản: v").append(version.getVersionNo()).append(" (").append(version.getOriginalFilename()).append(")\n\n");

        // 2. Physical document text extraction
        String extractedDocText = "";
        try {
            Resource fileResource = fileStorageService.loadFileAsResource(version.getStoredFilename());
            if (fileResource != null && fileResource.isReadable()) {
                String filename = version.getOriginalFilename() != null ? version.getOriginalFilename().toLowerCase() : "";
                if (filename.endsWith(".docx")) {
                    try (InputStream is = fileResource.getInputStream()) {
                        extractedDocText = extractFromDocx(is);
                    }
                } else if (filename.endsWith(".txt")) {
                    try (InputStream is = fileResource.getInputStream()) {
                        extractedDocText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract physical text from plan file: {}", e.getMessage());
        }

        sb.append("--- NỘI DUNG VĂN BẢN TRÍCH XUẤT ---\n");
        if (!extractedDocText.isBlank()) {
            // Capped at 15,000 chars to avoid exceeding token limits while having plenty of content
            if (extractedDocText.length() > 15000) {
                sb.append(extractedDocText.substring(0, 15000)).append("\n...[Đã lược bớt phần còn lại]...");
            } else {
                sb.append(extractedDocText);
            }
        } else {
            sb.append("[CẢNH BÁO KỸ THUẬT: Không thể trích xuất nội dung văn bản từ tệp ")
              .append(version.getOriginalFilename())
              .append(". Tệp có thể là tệp rỗng, định dạng không hỗ trợ bóc tách chữ tự động hoặc tệp ảnh quét].");
        }

        return sb.toString();
    }

    private String extractFromDocx(InputStream inputStream) {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String xml = baos.toString(StandardCharsets.UTF_8);
                    return cleanXmlToPlainText(xml);
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing docx zip structure: {}", e.getMessage());
        }
        return "";
    }

    private String cleanXmlToPlainText(String xml) {
        if (xml == null || xml.isBlank()) return "";
        return xml.replaceAll("<w:p.*?>", "\n")
                  .replaceAll("<w:tab/>", "    ")
                  .replaceAll("<.*?>", "")
                  .replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&amp;", "&")
                  .replaceAll("&quot;", "\"")
                  .replaceAll("&apos;", "'")
                  .replaceAll("[ \t]+", " ")
                  .replaceAll("\n{3,}", "\n\n")
                  .trim();
    }
}
