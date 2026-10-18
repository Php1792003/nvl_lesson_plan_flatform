package com.school.lessonplan.ai;

import com.school.lessonplan.ai.dto.AiReviewResponse;
import com.school.lessonplan.common.exception.ResourceNotFoundException;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanRepository;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import com.school.lessonplan.lessonplan.LessonPlanVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewService.class);

    private final LessonPlanRepository lessonPlanRepository;
    private final LessonPlanVersionRepository versionRepository;
    private final LessonPlanTextExtractor textExtractor;
    private final GeminiApiClient geminiApiClient;
    private final PedagogicalAiEngine pedagogicalAiEngine;

    public AiReviewService(LessonPlanRepository lessonPlanRepository,
                           LessonPlanVersionRepository versionRepository,
                           LessonPlanTextExtractor textExtractor,
                           GeminiApiClient geminiApiClient,
                           PedagogicalAiEngine pedagogicalAiEngine) {
        this.lessonPlanRepository = lessonPlanRepository;
        this.versionRepository = versionRepository;
        this.textExtractor = textExtractor;
        this.geminiApiClient = geminiApiClient;
        this.pedagogicalAiEngine = pedagogicalAiEngine;
    }

    public AiReviewResponse generateReview(Long planId, Long versionId) {
        LessonPlan plan = lessonPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giáo án ID: " + planId));

        LessonPlanVersion version;
        if (versionId != null) {
            version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản ID: " + versionId));
        } else {
            version = versionRepository.findCurrentVersion(planId)
                    .orElseGet(() -> {
                        java.util.List<LessonPlanVersion> versions = versionRepository.findByPlanIdOrderByVersionNoDesc(planId);
                        if (versions.isEmpty()) {
                            throw new ResourceNotFoundException("Giáo án chưa có phiên bản nào");
                        }
                        return versions.get(0);
                    });
        }


        // 1. Extract plan content and metadata
        String content = textExtractor.extractContent(plan, version);

        // 2. Try Gemini first if configured
        if (geminiApiClient.isConfigured()) {
            try {
                log.info("Requesting AI review from Google Gemini for planId={} version={}", planId, version.getVersionNo());
                return geminiApiClient.reviewPlan(plan, version, content);
            } catch (Exception e) {
                log.warn("Gemini AI failed, falling back to Pedagogical Engine: {}", e.getMessage());
            }
        }

        // 3. Fallback to Pedagogical Engine (TT 15/2026 & TT 17/2025/TT-BGDĐT)
        log.info("Generating review with Pedagogical AI Engine (TT 15/2026 & TT 17/2025) for planId={} version={}", planId, version.getVersionNo());
        return pedagogicalAiEngine.analyze(plan, version, content);
    }
}
