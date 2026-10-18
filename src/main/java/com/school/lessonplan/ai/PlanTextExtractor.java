// src/main/java/com/school/lessonplan/ai/PlanTextExtractor.java
package com.school.lessonplan.ai;

import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.ExtractedPlanText;
import com.school.lessonplan.ai.exception.FileUnreadableException;
import com.school.lessonplan.lessonplan.FileStorageService;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class PlanTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PlanTextExtractor.class);

    private final FileStorageService fileStorageService;
    private final AiReviewProperties aiReviewProperties;

    public PlanTextExtractor(FileStorageService fileStorageService, AiReviewProperties aiReviewProperties) {
        this.fileStorageService = fileStorageService;
        this.aiReviewProperties = aiReviewProperties;
    }

    public ExtractedPlanText extract(LessonPlanVersion version) {
        String originalFilename = version.getOriginalFilename() != null ? version.getOriginalFilename() : "tailieu";
        String extension = getExtension(originalFilename);

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(version.getStoredFilename());
        } catch (Exception e) {
            log.error("Failed to load file resource for version {}: {}", version.getId(), e.getMessage());
            throw new FileUnreadableException("Không tìm thấy hoặc không mở được tệp lưu trữ (" + originalFilename + ")", e);
        }

        if (resource == null || !resource.isReadable()) {
            throw new FileUnreadableException("Tệp lưu trữ không thể đọc được (" + originalFilename + ")");
        }

        String rawExtractedText;
        int pageOrSlideCount = 0;

        try (InputStream is = resource.getInputStream()) {
            switch (extension.toLowerCase()) {
                case "pdf" -> {
                    byte[] bytes = is.readAllBytes();
                    try (PDDocument document = Loader.loadPDF(bytes)) {
                        pageOrSlideCount = document.getNumberOfPages();
                        PDFTextStripper stripper = new PDFTextStripper();
                        rawExtractedText = stripper.getText(document);
                    }
                }
                case "docx" -> {
                    try (XWPFDocument document = new XWPFDocument(is);
                         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                        rawExtractedText = extractor.getText();
                        pageOrSlideCount = document.getProperties().getExtendedProperties().getPages();
                        if (pageOrSlideCount <= 0) {
                            pageOrSlideCount = 1;
                        }
                    }
                }
                case "pptx" -> {
                    try (XMLSlideShow slideShow = new XMLSlideShow(is);
                         SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
                        pageOrSlideCount = slideShow.getSlides().size();
                        rawExtractedText = extractor.getText();
                    }
                }
                case "txt" -> {
                    rawExtractedText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    pageOrSlideCount = 1;
                }
                default -> {
                    // Try Apache POI / fallback
                    log.warn("Unsupported or legacy extension for deep parsing: {}", extension);
                    throw new FileUnreadableException("Định dạng tệp ." + extension + " chưa hỗ trợ bóc tách văn bản tự động.");
                }
            }
        } catch (FileUnreadableException fe) {
            throw fe;
        } catch (Exception e) {
            log.error("Error reading file {} ({}): {}", originalFilename, version.getStoredFilename(), e.getMessage(), e);
            throw new FileUnreadableException("Không thể đọc được tệp " + originalFilename + ". Tệp có thể bị hỏng hoặc lỗi định dạng.", e);
        }

        if (rawExtractedText == null) {
            rawExtractedText = "";
        }

        // Clean text and calculate textLength after removing redundant whitespaces
        String normalizedClean = rawExtractedText.replaceAll("\\s+", " ").trim();
        int textLength = normalizedClean.length();

        // Limit characters sent to AI
        int maxChars = aiReviewProperties.getMaxChars();
        String textToSend = rawExtractedText;
        if (textToSend.length() > maxChars) {
            int remaining = textToSend.length() - maxChars;
            textToSend = textToSend.substring(0, maxChars) + "\n\n[ĐÃ CẮT BỚT NỘI DUNG: còn " + remaining + " ký tự không gửi]";
        }

        return new ExtractedPlanText(textToSend, textLength, pageOrSlideCount, originalFilename, extension);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            return filename.substring(dot + 1).toLowerCase();
        }
        return "";
    }
}
