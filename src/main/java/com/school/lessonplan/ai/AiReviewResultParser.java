// src/main/java/com/school/lessonplan/ai/AiReviewResultParser.java
package com.school.lessonplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AiReviewResultParser {

    private static final Logger log = LoggerFactory.getLogger(AiReviewResultParser.class);

    private final ObjectMapper objectMapper;
    private final AiReviewProperties properties;

    public AiReviewResultParser(ObjectMapper objectMapper, AiReviewProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public AiReviewResponse parseAndValidate(String rawAiOutput) {
        if (rawAiOutput == null || rawAiOutput.isBlank()) {
            throw new IllegalArgumentException("Dữ liệu phản hồi từ AI rỗng.");
        }

        String cleanJson = stripMarkdownCodeBlocks(rawAiOutput);

        try {
            JsonNode root = objectMapper.readTree(cleanJson);

            // 1. Extract and map input_type
            String inputTypeStr = root.path("input_type").asText(
                    root.path("inputType").asText("GIAO_AN_KHONG_DAY_DU")
            );
            InputType inputType = InputType.fromString(inputTypeStr);

            // 2. Extract and map verdict
            String verdictStr = root.path("verdict").asText("KHONG_DUYET");
            ReviewVerdict verdict = ReviewVerdict.fromString(verdictStr);

            // 3. Extract strings with sensible fallbacks
            String badgeText = root.path("badge_text").asText(
                    root.path("badgeText").asText("Chưa xác định")
            ).trim();

            String summary = root.path("summary").asText("").trim();
            if (summary.isBlank()) {
                summary = "Kế hoạch bài dạy đã được thẩm định.";
            }

            String conclusionText = root.path("conclusion_text").asText(
                    root.path("conclusionText").asText("")
            ).trim();

            String bghFollowUp = root.path("bgh_follow_up").asText(
                    root.path("bghFollowUp").asText("")
            ).trim();

            // 4. Extract passed_points
            List<String> passedPoints = new ArrayList<>();
            JsonNode passedNode = root.has("passed_points") ? root.get("passed_points") : root.get("passedPoints");
            if (passedNode != null && passedNode.isArray()) {
                for (JsonNode item : passedNode) {
                    String pt = item.asText("").trim();
                    if (!pt.isBlank()) {
                        passedPoints.add(pt);
                    }
                }
            }

            // 5. Extract checklist
            List<ChecklistItem> checklist = new ArrayList<>();
            JsonNode checklistNode = root.get("checklist");
            boolean hasSevereError = false;

            if (checklistNode != null && checklistNode.isArray()) {
                for (JsonNode item : checklistNode) {
                    String levelStr = item.path("level").asText("GOI_Y");
                    ChecklistLevel level = ChecklistLevel.fromString(levelStr);
                    if (level == ChecklistLevel.LOI_NGHIEM_TRONG) {
                        hasSevereError = true;
                    }
                    String issue = item.path("issue").asText("").trim();
                    String fix = item.path("fix").asText("").trim();
                    if (!issue.isBlank() || !fix.isBlank()) {
                        checklist.add(new ChecklistItem(level, issue, fix));
                    }
                }
            }

            // =========================================================================
            // BẮT BUỘC: Quy tắc nhất quán (Mục 7.5)
            // =========================================================================

            // Quy tắc 1: input_type ∈ {FILE_TRONG, SAI_LOAI_TAI_LIEU} thì ép verdict = KHONG_DUYET và passed_points = []
            if (inputType == InputType.FILE_TRONG || inputType == InputType.SAI_LOAI_TAI_LIEU) {
                verdict = ReviewVerdict.KHONG_DUYET;
                passedPoints.clear();
                if (badgeText.isBlank() || !badgeText.toLowerCase().contains("không đạt")) {
                    badgeText = (inputType == InputType.FILE_TRONG)
                            ? "Không đọc được nội dung tệp"
                            : "Không đạt: tệp không phải giáo án";
                }
            }

            // Quy tắc 2: verdict = DUYET mà checklist có LOI_NGHIEM_TRONG thì hạ xuống DUYET_CO_DIEU_KIEN hoặc KHONG_DUYET
            if (verdict == ReviewVerdict.DUYET && hasSevereError) {
                log.warn("Consistency rule applied: Downgrading verdict from DUYET to KHONG_DUYET due to LOI_NGHIEM_TRONG in checklist.");
                verdict = ReviewVerdict.KHONG_DUYET;
                if (!badgeText.toLowerCase().contains("chưa đạt") && !badgeText.toLowerCase().contains("không đạt")) {
                    badgeText = "Không đạt: có lỗi chuyên môn nghiêm trọng";
                }
            }

            if (conclusionText.isBlank()) {
                conclusionText = "Kính gửi Thầy/Cô,\n\nBan Giám Hiệu đã thẩm định kế hoạch bài dạy. KẾT LUẬN: "
                        + verdict.name() + ".\n\nTrân trọng,\nBan Giám Hiệu.";
            }

            AiReviewResponse response = new AiReviewResponse();
            response.setSuccess(true);
            response.setInputType(inputType);
            response.setVerdict(verdict);
            response.setBadgeText(badgeText);
            response.setSummary(summary);
            response.setPassedPoints(passedPoints);
            response.setChecklist(checklist);
            response.setConclusionText(conclusionText);
            response.setBghFollowUp(bghFollowUp);
            response.setSourceLabel(properties.getSourceLabel());

            return response;

        } catch (Exception e) {
            log.error("Failed to parse AI review output: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Dữ liệu phản hồi từ AI không đúng cấu trúc JSON: " + e.getMessage(), e);
        }
    }

    public static String stripMarkdownCodeBlocks(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
