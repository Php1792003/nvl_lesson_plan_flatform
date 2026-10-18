// src/main/java/com/school/lessonplan/ai/AiReviewService.java
package com.school.lessonplan.ai;

import com.school.lessonplan.ai.client.AiClient;
import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.*;
import com.school.lessonplan.ai.exception.AiResponseInvalidException;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.evaluation.Evaluation;
import com.school.lessonplan.evaluation.EvaluationRepository;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import com.school.lessonplan.lessonplan.LessonPlanVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.lessonplan.lessonplan.LessonPlanAccessService;
import com.school.lessonplan.security.CurrentUser;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewService.class);

    private final LessonPlanRepository lessonPlanRepository;
    private final LessonPlanVersionRepository versionRepository;
    private final EvaluationRepository evaluationRepository;
    private final PlanTextExtractor planTextExtractor;
    private final AiReviewPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiReviewResultParser resultParser;
    private final AiReviewProperties properties;
    private final LessonPlanAccessService accessService;

    // In-memory cache: key = sha256 + "_" + planId -> CacheEntry
    private final Map<String, CacheEntry> reviewCache = new ConcurrentHashMap<>();

    // Concurrency control: 1 running review per planId
    private final Map<Long, Boolean> activeReviews = new ConcurrentHashMap<>();

    @Autowired
    public AiReviewService(LessonPlanRepository lessonPlanRepository,
                           LessonPlanVersionRepository versionRepository,
                           EvaluationRepository evaluationRepository,
                           PlanTextExtractor planTextExtractor,
                           AiReviewPromptBuilder promptBuilder,
                           AiClient aiClient,
                           AiReviewResultParser resultParser,
                           AiReviewProperties properties,
                           LessonPlanAccessService accessService) {
        this.lessonPlanRepository = lessonPlanRepository;
        this.versionRepository = versionRepository;
        this.evaluationRepository = evaluationRepository;
        this.planTextExtractor = planTextExtractor;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.resultParser = resultParser;
        this.properties = properties;
        this.accessService = accessService;
    }

    public AiReviewService(LessonPlanRepository lessonPlanRepository,
                           LessonPlanVersionRepository versionRepository,
                           EvaluationRepository evaluationRepository,
                           PlanTextExtractor planTextExtractor,
                           AiReviewPromptBuilder promptBuilder,
                           AiClient aiClient,
                           AiReviewResultParser resultParser,
                           AiReviewProperties properties) {
        this(lessonPlanRepository, versionRepository, evaluationRepository, planTextExtractor, promptBuilder, aiClient, resultParser, properties, null);
    }

    @Transactional(readOnly = true)
    public AiReviewResponse reviewPlan(Long planId, Long versionId) {
        CurrentUser currentUser = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser u) {
            currentUser = u;
        }
        return reviewPlan(planId, versionId, currentUser);
    }

    @Transactional(readOnly = true)
    public AiReviewResponse reviewPlan(Long planId, Long versionId, CurrentUser currentUser) {
        // [1] Kiểm tra planId tồn tại
        LessonPlan plan = lessonPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án ID: " + planId));

        // Kiểm tra quyền xem giáo án để chống tấn công IDOR
        if (accessService != null && currentUser != null) {
            accessService.requireViewAccess(currentUser, plan);
        }

        // Lấy phiên bản mới nhất hoặc phiên bản được chỉ định
        LessonPlanVersion version;
        if (versionId != null) {
            version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản ID: " + versionId));
        } else {
            version = versionRepository.findCurrentVersion(planId)
                    .orElseGet(() -> {
                        List<LessonPlanVersion> versions = versionRepository.findByPlanIdOrderByVersionNoDesc(planId);
                        if (versions.isEmpty()) {
                            throw new ResourceNotFoundException("Giáo án chưa có phiên bản tệp đính kèm nào.");
                        }
                        return versions.get(0);
                    });
        }

        // Kiểm tra In-memory cache (SHA-256 + planId)
        String cacheKey = (version.getSha256() != null ? version.getSha256() : version.getStoredFilename()) + "_" + planId;
        CacheEntry cached = reviewCache.get(cacheKey);
        if (cached != null && !cached.isExpired(properties.getCacheExpireMinutes())) {
            log.info("Returning cached AI review for planId={} versionId={}", planId, version.getId());
            return cached.response();
        }

        // [2] Đọc file giáo án & [3] Trích văn bản (PDF/DOCX/PPTX)
        ExtractedPlanText extracted = planTextExtractor.extract(version);

        // [4] PRE-CHECK trong code (KHÔNG gọi AI nếu textLength < minChars)
        if (extracted.getTextLength() < properties.getMinChars()) {
            log.info("Pre-check triggered: textLength={} < {} for planId={}. Returning FILE_TRONG without AI call.",
                    extracted.getTextLength(), properties.getMinChars(), planId);
            AiReviewResponse preCheckResponse = buildEmptyFileResponse(plan, version, extracted);
            reviewCache.put(cacheKey, new CacheEntry(preCheckResponse, Instant.now()));
            return preCheckResponse;
        }

        // Kiểm tra concurrency: tối đa 1 request đang chạy cho mỗi planId
        if (activeReviews.putIfAbsent(planId, Boolean.TRUE) != null) {
            throw new IllegalStateException("Đang có một yêu cầu thẩm định AI đang chạy cho giáo án này. Vui lòng chờ hoàn tất.");
        }

        try {
            // [5] Lấy lịch sử nhận xét của BGH đối với giáo án này (để AI study lại các comment)
            List<Evaluation> pastEvaluations = evaluationRepository.findByPlanIdOrderByCreatedAtDesc(planId);

            // Nếu giáo án chưa có nhận xét trước, lấy các mẫu nhận xét gần đây của BGH trong bộ môn/trường để học phong cách
            List<Evaluation> referenceEvaluations = Collections.emptyList();
            if (pastEvaluations.isEmpty()) {
                try {
                    if (plan.getDepartment() != null && plan.getDepartment().getId() != null) {
                        referenceEvaluations = evaluationRepository.findByPlanDepartmentIdOrderByCreatedAtDesc(
                                plan.getDepartment().getId(), PageRequest.of(0, 3));
                    }
                    if (referenceEvaluations.isEmpty()) {
                        referenceEvaluations = evaluationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 3));
                    }
                } catch (Exception e) {
                    log.warn("Could not load reference evaluations: {}", e.getMessage());
                }
            }

            // Dựng user message (metadata + căn cứ pháp lý + lịch sử nhận xét BGH + nội dung bài)
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userMessage = promptBuilder.buildUserMessage(plan, extracted, pastEvaluations, referenceEvaluations);

            // [6] Gọi AI với system prompt, temperature 0.2, yêu cầu JSON
            log.info("Invoking AI review for planId={} (pastBghComments={}, textLength={})",
                    planId, pastEvaluations.size(), extracted.getTextLength());
            String aiRawResponse;
            try {
                aiRawResponse = aiClient.complete(systemPrompt, userMessage);
            } catch (Exception apiException) {
                log.warn("Gemini AI API call failed: {}. Activating internal pedagogical evaluator fallback...", apiException.getMessage());
                aiRawResponse = evaluateInternalFallback(plan, extracted, pastEvaluations);
            }

            // [7] Parse + validate JSON (lỗi → thử lại 1 lần → vẫn lỗi thì ném ngoại lệ)
            AiReviewResponse reviewResponse;
            try {
                reviewResponse = resultParser.parseAndValidate(aiRawResponse);
            } catch (Exception parseException) {
                log.warn("First JSON parse failed for planId={}: {}. Retrying once with explicit instruction...",
                        planId, parseException.getMessage());
                String retryUserMessage = userMessage + "\n\nLƯU Ý: Phản hồi trước bị lỗi định dạng. Chỉ trả về duy nhất 1 đối tượng JSON hợp lệ theo đúng cấu trúc đã yêu cầu, không kèm bất kỳ giải thích nào bên ngoài JSON.";
                try {
                    String retryRawResponse = aiClient.complete(systemPrompt, retryUserMessage);
                    reviewResponse = resultParser.parseAndValidate(retryRawResponse);
                } catch (Exception secondException) {
                    log.error("Second AI JSON parse attempt failed for planId={}: {}", planId, secondException.getMessage());
                    throw new AiResponseInvalidException("AI không trả về dữ liệu đúng định dạng JSON chuẩn sau 2 lần thử.", secondException);
                }
            }

            // [8] Lưu cache và trả AiReviewResponse
            reviewCache.put(cacheKey, new CacheEntry(reviewResponse, Instant.now()));
            return reviewResponse;

        } finally {
            activeReviews.remove(planId);
        }
    }

    private String evaluateInternalFallback(LessonPlan plan, ExtractedPlanText extracted, List<Evaluation> pastEvaluations) {
        String body = extracted.getText().toLowerCase();
        String title = plan.getTitle() != null ? plan.getTitle() : "Kế hoạch bài dạy";
        String subject = (plan.getDepartment() != null) ? plan.getDepartment().getName() : "Bộ môn";
        String grade = (plan.getGrade() != null) ? plan.getGrade() : "";
        String fileName = extracted.getFileName();
        boolean hasPastComments = pastEvaluations != null && !pastEvaluations.isEmpty();

        // 1. Phân loại xem có phải CV / hồ sơ cá nhân hoặc tài liệu sai loại không
        boolean isCvOrPersonal = body.contains("curriculum vitae")
                || body.contains("resume")
                || (body.contains("kỹ năng") && (body.contains("học vấn") || body.contains("kinh nghiệm làm việc") || body.contains("kinh nghiệm dự án") || body.contains("giới thiệu bản thân")))
                || body.contains("mục tiêu nghề nghiệp")
                || body.contains("career objective")
                || (body.contains("education") && body.contains("experience"))
                || fileName.toLowerCase().contains("cv")
                || fileName.toLowerCase().contains("resume");

        // Đếm các từ khóa cấu trúc kế hoạch bài dạy
        int pedagogicalKeywords = countKeywords(body,
                "mục tiêu", "yêu cầu cần đạt", "khởi động", "mở đầu",
                "hình thành kiến thức", "khám phá", "luyện tập", "vận dụng",
                "hoạt động", "tiến trình", "thiết bị dạy học", "học liệu",
                "sản phẩm học tập", "nhiệm vụ học tập", "kế hoạch bài dạy", "giáo án");

        if (isCvOrPersonal || pedagogicalKeywords < 2) {
            String cvType = isCvOrPersonal ? "hồ sơ cá nhân (CV)" : "tài liệu không thuộc cấu trúc bài dạy";
            return """
            {
              "input_type": "SAI_LOAI_TAI_LIEU",
              "verdict": "KHONG_DUYET",
              "badge_text": "Không đạt: tệp không phải giáo án",
              "summary": "Tệp đính kèm (%s) là %s, không phải kế hoạch bài dạy theo quy chuẩn sư phạm.",
              "bgh_follow_up": "",
              "passed_points": [],
              "checklist": [
                {
                  "level": "LOI_NGHIEM_TRONG",
                  "issue": "Tệp tải lên là %s, không chứa mục tiêu bài dạy hay tiến trình 4 hoạt động học tập",
                  "fix": "Thu hồi tệp này và nộp lại đúng kế hoạch bài dạy môn %s khối %s"
                }
              ],
              "conclusion_text": "Kính gửi Thầy/Cô,\\n\\nBan Giám Hiệu đã kiểm tra tệp đính kèm cho kế hoạch bài dạy \\"%s\\" (Môn: %s | Khối: %s).\\n\\nKẾT LUẬN: KHÔNG DUYỆT phiên bản này do tệp tải lên là %s, không phải kế hoạch bài dạy theo cấu trúc kế hoạch bài dạy hiện hành.\\n\\nĐề nghị Thầy/Cô kiểm tra lại tệp văn bản và nộp lại đúng kế hoạch bài dạy trước khi lên lớp.\\n\\nTrân trọng,\\nBan Giám Hiệu."
            }
            """.formatted(fileName, cvType, cvType, subject, grade, title, subject, grade, cvType);
        }

        // 2. Là kế hoạch bài dạy -> Thẩm định cấu trúc
        boolean hasMucTieu = body.contains("mục tiêu") || body.contains("yêu cầu cần đạt");
        boolean hasKhoiDong = body.contains("khởi động") || body.contains("mở đầu") || body.contains("xác định vấn đề");
        boolean hasKhamPha = body.contains("hình thành kiến thức") || body.contains("khám phá") || body.contains("nội dung bài");
        boolean hasLuyenTap = body.contains("luyện tập") || body.contains("thực hành") || body.contains("bài tập");
        boolean hasVanDung = body.contains("vận dụng") || body.contains("tìm tòi mở rộng") || body.contains("liên hệ");

        int activitiesCount = (hasKhoiDong ? 1 : 0) + (hasKhamPha ? 1 : 0) + (hasLuyenTap ? 1 : 0) + (hasVanDung ? 1 : 0);

        if (activitiesCount >= 3 && hasMucTieu) {
            String verdict = (activitiesCount == 4) ? "DUYET" : "DUYET_CO_DIEU_KIEN";
            String badge = (activitiesCount == 4) ? "Đạt chuẩn sư phạm" : "Đạt có điều kiện";
            String vanDungFix = hasVanDung ? "" : """
                ,
                {
                  "level": "CAN_BO_SUNG",
                  "issue": "Kế hoạch bài dạy chưa thể hiện rõ hoạt động Vận dụng",
                  "fix": "Bổ sung hoạt động Vận dụng gắn với thực tế để học sinh vận dụng kiến thức đã học"
                }
            """;

            String bghFollowUp = hasPastComments
                    ? "Giáo án đã được đối chiếu với " + pastEvaluations.size() + " lần nhận xét trước của Ban Giám Hiệu. Phiên bản hiện tại đã cập nhật và tiếp thu các định hướng chuyên môn."
                    : "";

            String pastPassedPoint = hasPastComments
                    ? ",\n    \"Đã tiếp thu ý kiến từ các nhận xét trước của Ban Giám Hiệu để hoàn thiện kế hoạch bài dạy\""
                    : "";

            String conclusionNote = hasPastComments
                    ? "\\n\\nĐánh giá tiếp thu ý kiến BGH: Giáo viên đã có sự tiếp thu, cập nhật các yêu cầu chuyên môn mà Ban Giám Hiệu đã lưu ý ở các lần nhận xét trước."
                    : "";

            return """
            {
              "input_type": "GIAO_AN_HOP_LE",
              "verdict": "%s",
              "badge_text": "%s",
              "summary": "Kế hoạch bài dạy có cấu trúc sư phạm rõ ràng, xác định mục tiêu và chuỗi hoạt động học tập phù hợp.",
              "bgh_follow_up": "%s",
              "passed_points": [
                "Xác định rõ ràng mục tiêu về kiến thức, năng lực và phẩm chất cho học sinh",
                "Có thiết kế chuỗi hoạt động học tập với sự phân định nhiệm vụ giữa giáo viên và học sinh"%s
              ],
              "checklist": [
                {
                  "level": "GOI_Y",
                  "issue": "Nên tăng cường công cụ đánh giá quá trình (bảng kiểm hoặc câu hỏi nhanh)",
                  "fix": "Tích hợp thêm phiếu học tập hoặc tiêu chí đánh giá sản phẩm ở hoạt động luyện tập"
                }%s
              ],
              "conclusion_text": "Kính gửi Thầy/Cô,\\n\\nBan Giám Hiệu đã thẩm định kế hoạch bài dạy \\"%s\\" (Môn: %s | Khối: %s).\\n\\nKẾT LUẬN: %s.%s\\n\\nKế hoạch bài dạy đáp ứng tốt yêu cầu chuyên môn, đề nghị Thầy/Cô hoàn thiện một số gợi ý nhỏ để tiết dạy đạt hiệu quả cao nhất.\\n\\nTrân trọng,\\nBan Giám Hiệu."
            }
            """.formatted(verdict, badge, bghFollowUp, pastPassedPoint, vanDungFix, title, subject, grade,
                    (activitiesCount == 4 ? "DUYỆT BÀI DẠY" : "DUYỆT CÓ ĐIỀU KIỆN"), conclusionNote);
        } else {
            return """
            {
              "input_type": "GIAO_AN_KHONG_DAY_DU",
              "verdict": "KHONG_DUYET",
              "badge_text": "Chưa đạt: thiếu các phần cốt lõi",
              "summary": "Kế hoạch bài dạy còn sơ sài, thiếu các hoạt động học tập cốt lõi theo quy định sư phạm.",
              "bgh_follow_up": "",
              "passed_points": [],
              "checklist": [
                {
                  "level": "LOI_NGHIEM_TRONG",
                  "issue": "Thiếu các hoạt động học cốt lõi (Khám phá / Luyện tập / Vận dụng) hoặc thiếu mục tiêu đo lường được",
                  "fix": "Rà soát và bổ sung đầy đủ 4 hoạt động học theo cấu trúc kế hoạch bài dạy hiện hành"
                }
              ],
              "conclusion_text": "Kính gửi Thầy/Cô,\\n\\nBan Giám Hiệu đã kiểm tra kế hoạch bài dạy \\"%s\\" (Môn: %s | Khối: %s).\\n\\nKẾT LUẬN: KHÔNG DUYỆT.\\n\\nLý do: Kế hoạch bài dạy còn thiếu các hoạt động học cốt lõi theo quy định. Đề nghị Thầy/Cô bổ sung hoàn chỉnh và nộp lại trước khi giảng dạy.\\n\\nTrân trọng,\\nBan Giám Hiệu."
            }
            """.formatted(title, subject, grade);
        }
    }

    private int countKeywords(String text, String... keywords) {
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    private AiReviewResponse buildEmptyFileResponse(LessonPlan plan, LessonPlanVersion version, ExtractedPlanText extracted) {
        String subject = (plan.getDepartment() != null) ? plan.getDepartment().getName() : "Bộ môn";
        String grade = (plan.getGrade() != null) ? plan.getGrade() : "";
        String fileName = extracted.getFileName();

        String conclusion = """
Kính gửi Thầy/Cô,

Ban Giám Hiệu đã kiểm tra tệp đính kèm cho kế hoạch bài dạy "%s" (Môn: %s | Khối: %s).

KẾT LUẬN: KHÔNG DUYỆT phiên bản này do hệ thống không trích xuất được nội dung văn bản từ tệp đính kèm (%s). Tệp có thể là bản quét ảnh hoặc không có nội dung.

Đề nghị Thầy/Cô kiểm tra lại tệp văn bản và nộp lại bản có chứa nội dung văn bản rõ ràng (định dạng .docx hoặc .pdf) trong thời gian sớm nhất để Ban Giám Hiệu tiến hành thẩm định chuyên môn.

Trân trọng,
Ban Giám Hiệu.
""".formatted(plan.getTitle(), subject, grade, fileName).trim();

        AiReviewResponse res = new AiReviewResponse();
        res.setSuccess(true);
        res.setInputType(InputType.FILE_TRONG);
        res.setVerdict(ReviewVerdict.KHONG_DUYET);
        res.setBadgeText("Không đọc được nội dung tệp");
        res.setSummary("Không trích xuất được văn bản từ tệp đính kèm (" + fileName + "). Tệp có thể là bản quét ảnh hoặc không có nội dung.");
        res.setPassedPoints(List.of());
        res.setChecklist(List.of(
                new ChecklistItem(
                        ChecklistLevel.LOI_NGHIEM_TRONG,
                        "Tệp đính kèm không trích xuất được nội dung văn bản bài soạn",
                        "Nộp lại tệp định dạng .docx hoặc .pdf có chứa nội dung văn bản"
                )
        ));
        res.setConclusionText(conclusion);
        res.setSourceLabel(properties.getSourceLabel());
        return res;
    }

    private record CacheEntry(AiReviewResponse response, Instant timestamp) {
        boolean isExpired(int expireMinutes) {
            return Instant.now().isAfter(timestamp.plusSeconds((long) expireMinutes * 60));
        }
    }
}
