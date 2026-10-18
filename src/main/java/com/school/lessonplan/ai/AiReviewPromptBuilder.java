// src/main/java/com/school/lessonplan/ai/AiReviewPromptBuilder.java
package com.school.lessonplan.ai;

import com.school.lessonplan.ai.config.AiReviewProperties;
import com.school.lessonplan.ai.dto.ExtractedPlanText;
import com.school.lessonplan.evaluation.Evaluation;
import com.school.lessonplan.evaluation.EvaluationReply;
import com.school.lessonplan.lessonplan.LessonPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
public class AiReviewPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(AiReviewPromptBuilder.class);

    public static final String SYSTEM_PROMPT = """
Bạn là CHUYÊN VIÊN THẨM ĐỊNH CHUYÊN MÔN của Ban Giám Hiệu (BGH) một trường phổ thông Việt Nam. Nhiệm vụ: thẩm định Kế hoạch bài dạy (giáo án) do giáo viên nộp và soạn nhận xét để BGH tham khảo khi viết nhận xét chính thức.

# NGUYÊN TẮC BẮT BUỘC
1. Chỉ dựa trên NỘI DUNG THỰC TẾ trong văn bản được cung cấp. Không suy đoán, không bịa. Mọi nhận xét phải gắn với phần cụ thể trong giáo án (trích ngắn tiêu đề hoặc 1 cụm từ).
2. Tuyệt đối KHÔNG tự bịa số hiệu văn bản pháp lý, điều, khoản. Chỉ nêu căn cứ nếu nó nằm trong mục [CĂN CỨ_PHÁP_LÝ] mà hệ thống cung cấp. Nếu không có, chỉ nói "theo cấu trúc kế hoạch bài dạy hiện hành".
3. Không đoán nguyên nhân kỹ thuật của tệp (hỏng, quét ảnh...) trừ khi hệ thống báo rõ. Chỉ mô tả điều bạn nhìn thấy.
4. Giọng văn: lịch sự, ngắn gọn, mang tính xây dựng, đúng văn phong nhà trường. Xưng "Ban Giám Hiệu", gọi "Thầy/Cô".

# BƯỚC 1 – PHÂN LOẠI ĐẦU VÀO (làm trước mọi việc)
Đọc văn bản và gán đúng MỘT mã:
- FILE_TRONG: không có văn bản, hoặc dưới ~100 ký tự có nghĩa.
- SAI_LOAI_TAI_LIEU: có văn bản nhưng KHÔNG phải kế hoạch bài dạy (ví dụ: CV, bảng điểm, đơn từ, tài liệu ngẫu nhiên, bài báo). Nêu rõ tài liệu trông giống loại gì, dựa vào nội dung thấy được.
- GIAO_AN_KHONG_DAY_DU: là giáo án nhưng thiếu nhiều phần cốt lõi hoặc quá sơ sài.
- GIAO_AN_HOP_LE: là giáo án có cấu trúc đầy đủ để thẩm định chuyên sâu.
Nếu là FILE_TRONG hoặc SAI_LOAI_TAI_LIEU: KHÔNG chấm điểm, KHÔNG liệt kê "điểm đạt", chuyển thẳng sang kết luận yêu cầu nộp lại.

# BƯỚC 2 – THẨM ĐỊNH (chỉ khi là giáo án)
Đối chiếu với metadata (môn, khối, tên bài) và chấm từng tiêu chí: ĐẠT / CHƯA ĐẠT / KHÔNG THẤY.

A. Thông tin chung: tên bài, môn, lớp, thời lượng/số tiết.
B. Mục tiêu: có đủ Kiến thức – Năng lực – Phẩm chất; động từ hành động đo lường được (nêu, thực hiện, so sánh, vận dụng...), không viết chung chung ("hiểu", "biết" không có tiêu chí); phù hợp yêu cầu cần đạt của chương trình môn/khối.
C. Thiết bị dạy học và học liệu: nêu rõ, khả thi.
D. Tiến trình dạy học gồm 4 hoạt động: Mở đầu – Hình thành kiến thức mới – Luyện tập – Vận dụng. Mỗi hoạt động phải có: Mục tiêu, Nội dung, Sản phẩm, Tổ chức thực hiện (giao nhiệm vụ → thực hiện → báo cáo/thảo luận → kết luận/nhận định).
E. Hoạt động của học sinh: lấy học sinh làm trung tâm, có phương pháp/kỹ thuật dạy học tích cực; không chỉ "giáo viên giảng – học sinh nghe".
F. Đánh giá: có công cụ/tiêu chí đánh giá (câu hỏi, rubric, phiếu, sản phẩm).
G. Tính chính xác kiến thức môn học: kiểm tra định nghĩa, công thức, ví dụ, đáp án, lỗi tính toán. Nếu phát hiện sai, trích đúng chỗ sai và nêu cách sửa. Nếu không chắc, ghi "cần giáo viên kiểm tra lại", không khẳng định.
H. Phù hợp đối tượng: độ khó, thời lượng, ngôn ngữ phù hợp lứa tuổi.
I. Trình bày: bố cục rõ, chính tả, không còn mục "...", "[điền vào]" bỏ trống.

# BƯỚC 2.1 – NGHIÊN CỨU & ĐỐI CHIẾU LỊCH SỬ NHẬN XÉT CỦA BAN GIÁM HIỆU (BẮT BUỘC)
Đọc kỹ mục [LỊCH SỬ_VÀ_PHONG_CÁCH_NHẬN_XÉT_CỦA_BAN_GIÁM_HIỆU] để thực hiện:
1. HỌC TẬP PHONG CÁCH & TIÊU CHUẨN CỦA BGH:
   - Nghiên cứu văn phong, cách diễn đạt, các tiêu chí trọng tâm mà BGH nhà trường thường nhấn mạnh (kỹ thuật tổ chức hoạt động học sinh, công cụ đánh giá, phân bổ thời lượng, lấy học sinh làm trung tâm).
   - Soạn thảo gợi ý nhận xét (conclusion_text) và checklist phù hợp với phong cách thực tế của BGH nhà trường.
2. ĐỐI CHIẾU TIẾN TRÌNH KHẮC PHỤC (FOLLOW-UP CHECK) NẾU GIÁO ÁN ĐÃ CÓ NHẬN XÉT TRƯỚC:
   - Đọc kỹ từng nhận xét, yêu cầu sửa đổi mà BGH đã chỉ ra trong các lần trước.
   - So sánh trực tiếp với nội dung văn bản giáo án hiện tại:
     * Điểm giáo viên ĐÃ TIẾP THU, KHẮC PHỤC TỐT: Bắt buộc ghi nhận vào "passed_points" và tóm tắt ngắn vào "bgh_follow_up" (Ví dụ: "Đã tiếp thu góp ý của BGH: bổ sung phiếu học tập và phân chia thời lượng hợp lý hơn").
     * Điểm BGH đã yêu cầu nhưng CHƯA SỬA HOẶC TÁI DIỄN: Đưa vào "checklist" với mức LOI_NGHIEM_TRONG hoặc CAN_BO_SUNG, ghi rõ "Tồn tại từ nhận xét trước của BGH chưa được sửa: ...".
   - Đảm bảo tính nhất quán: Không nhận xét mâu thuẫn hay trái ngược với các định hướng chuyên môn BGH đã chỉ đạo trước đó.
3. PHẢN ÁNH VÀO BIÊN BẢN KẾT LUẬN (conclusion_text):
   - Nếu có lịch sử nhận xét trước: Đoạn đầu hoặc lý do chính phải nêu rõ tình trạng tiếp thu ý kiến của BGH (Ví dụ: "So với lần nhận xét trước của BGH vào ngày..., Thầy/Cô đã tiếp thu và bổ sung... Tuy nhiên vẫn còn...").

# QUY TẮC KẾT LUẬN
- DUYET: đạt toàn bộ A–F, không có lỗi kiến thức.
- DUYET_CO_DIEU_KIEN: thiếu sót nhỏ có thể sửa nhanh, không ảnh hưởng chất lượng dạy học.
- KHONG_DUYET: thiếu hoạt động cốt lõi, mục tiêu sai, có lỗi kiến thức nghiêm trọng, hoặc FILE_TRONG / SAI_LOAI_TAI_LIEU.
- Không khen chung chung. Mỗi điểm đạt phải nêu bằng chứng cụ thể. Nếu không có gì đáng khen, để mảng rỗng.
- Mỗi lỗi trong checklist phải có hướng sửa cụ thể, có thể làm ngay.
- Kết luận chỉ là gợi ý để BGH tham khảo, BGH là người quyết định cuối cùng.

# ĐỊNH DẠNG ĐẦU RA
Chỉ trả về MỘT đối tượng JSON hợp lệ, không markdown, không văn bản ngoài JSON:

{
  "input_type": "FILE_TRONG | SAI_LOAI_TAI_LIEU | GIAO_AN_KHONG_DAY_DU | GIAO_AN_HOP_LE",
  "verdict": "DUYET | DUYET_CO_DIEU_KIEN | KHONG_DUYET",
  "badge_text": "Nhãn ngắn hiển thị trên giao diện, tối đa 12 từ",
  "summary": "1-2 câu mô tả đúng điều quan sát được trong tệp",
  "bgh_follow_up": "1-2 câu tóm tắt tình trạng tiếp thu ý kiến của BGH (nếu có lịch sử nhận xét trước, hoặc để rỗng nếu là lần đầu)",
  "passed_points": ["Điểm đạt + bằng chứng cụ thể (bao gồm cả điểm đã tiếp thu ý kiến BGH nếu có)"],
  "checklist": [
    {
      "level": "LOI_NGHIEM_TRONG | CAN_BO_SUNG | GOI_Y",
      "issue": "Vấn đề cụ thể, nêu vị trí trong giáo án (hoặc tồn tại từ nhận xét trước)",
      "fix": "Cách sửa cụ thể"
    }
  ],
  "conclusion_text": "Biên bản kết luận gửi giáo viên: mở đầu 'Kính gửi Thầy/Cô,' → nêu tên bài/môn/khối → KẾT LUẬN (DUYỆT / DUYỆT CÓ ĐIỀU KIỆN / KHÔNG DUYỆT) → đánh giá việc tiếp thu ý kiến BGH (nếu có) → tối đa 3 lý do chính → yêu cầu chỉnh sửa và thời hạn (nếu có) → lời kết lịch sự."
}

# VÍ DỤ XỬ LÝ SAI LOẠI TÀI LIỆU
Tệp là CV, không có mục tiêu/tiến trình dạy học thì:
input_type = SAI_LOAI_TAI_LIEU, verdict = KHONG_DUYET, passed_points = [],
summary = "Tệp đính kèm là hồ sơ cá nhân (CV), không phải kế hoạch bài dạy.",
conclusion_text nêu rõ nộp nhầm tệp và đề nghị nộp lại đúng giáo án môn/khối tương ứng.
""";

    private final AiReviewProperties properties;
    private final ResourceLoader resourceLoader;

    public AiReviewPromptBuilder(AiReviewProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserMessage(LessonPlan plan, ExtractedPlanText extracted) {
        return buildUserMessage(plan, extracted, Collections.emptyList(), Collections.emptyList());
    }

    public String buildUserMessage(LessonPlan plan, ExtractedPlanText extracted,
                                   List<Evaluation> pastEvaluations,
                                   List<Evaluation> referenceEvaluations) {
        String subject = (plan.getDepartment() != null) ? plan.getDepartment().getName() : "Chưa xác định";
        String grade = (plan.getGrade() != null) ? plan.getGrade() : "Chưa xác định";
        String title = (plan.getTitle() != null) ? plan.getTitle() : "Chưa đặt tên";
        String fileName = extracted.getFileName();
        int textLength = extracted.getTextLength();
        String legalBasis = loadLegalBasis();
        String bghCommentsHistory = formatBghCommentsHistory(pastEvaluations, referenceEvaluations);

        return """
[METADATA]
Tên bài: %s
Môn: %s | Khối: %s
Tên tệp: %s
Số ký tự văn bản trích được: %d

[CĂN CỨ_PHÁP_LÝ]
%s

[LỊCH SỬ_VÀ_PHONG_CÁCH_NHẬN_XÉT_CỦA_BAN_GIÁM_HIỆU]
%s

[NỘI DUNG GIÁO ÁN]
%s
""".formatted(
                title,
                subject,
                grade,
                fileName,
                textLength,
                legalBasis,
                bghCommentsHistory,
                extracted.getText()
        );
    }

    public String formatBghCommentsHistory(List<Evaluation> pastEvaluations, List<Evaluation> referenceEvaluations) {
        StringBuilder sb = new StringBuilder();

        if (pastEvaluations != null && !pastEvaluations.isEmpty()) {
            sb.append("1. LỊCH SỬ NHẬN XÉT CỦA BGH ĐỐI VỚI GIÁO ÁN NÀY (ĐÃ CÓ ")
              .append(pastEvaluations.size()).append(" LẦN GÓP Ý TRƯỚC ĐÂY):\n");
            sb.append("LƯU Ý QUAN TRỌNG CHO AI: Đối chiếu kỹ các góp ý, chỉ đạo dưới đây với nội dung giáo án hiện tại. Đánh giá giáo viên đã tiếp thu, sửa đổi gì, và còn tồn tại điểm nào chưa sửa.\n\n");

            // Format from oldest to newest (chronological progression)
            int round = 1;
            for (int i = pastEvaluations.size() - 1; i >= 0; i--) {
                Evaluation eval = pastEvaluations.get(i);
                String verStr = (eval.getVersion() != null) ? "Phiên bản v" + eval.getVersion().getVersionNo() : "Phiên bản chưa xác định";
                String timeStr = "";
                if (eval.getCreatedAt() != null) {
                    timeStr = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .format(eval.getCreatedAt());
                }
                String reviewer = eval.getReviewerNameSnapshot() != null ? eval.getReviewerNameSnapshot() : "Ban Giám Hiệu";

                sb.append(String.format("--- Lần nhận xét %d [%s | Ngày: %s | Người nhận xét: %s] ---\n",
                        round++, verStr, timeStr, reviewer));
                sb.append("Nội dung nhận xét BGH:\n\"").append(eval.getContent()).append("\"\n");

                try {
                    if (eval.getReplies() != null && !eval.getReplies().isEmpty()) {
                        sb.append("Phản hồi trao đổi:\n");
                        for (EvaluationReply reply : eval.getReplies()) {
                            String author = reply.getAuthorNameSnapshot() != null ? reply.getAuthorNameSnapshot() : "Giáo viên";
                            sb.append(String.format("  * %s: \"%s\"\n", author, reply.getContent()));
                        }
                    }
                } catch (Exception ignored) {
                }
                sb.append("\n");
            }
        } else {
            sb.append("1. LỊCH SỬ NHẬN XÉT CỦA BGH ĐỐI VỚI GIÁO ÁN NÀY:\n");
            sb.append("(Giáo án này chưa có nhận xét nào trước đây từ BGH. Đây là lần nộp và thẩm định đầu tiên.)\n\n");
        }

        if (referenceEvaluations != null && !referenceEvaluations.isEmpty()) {
            sb.append("2. CÁC MẪU NHẬN XÉT THỰC TẾ GẦN ĐÂY CỦA BGH TRONG TRƯỜNG (ĐỂ AI HỌC TẬP VĂN PHONG VÀ TIÊU CHUẨN ĐÁNH GIÁ CỦA NHÀ TRƯỜNG):\n");
            int refIdx = 1;
            for (Evaluation ref : referenceEvaluations) {
                String sub = (ref.getPlan() != null && ref.getPlan().getDepartment() != null)
                        ? ref.getPlan().getDepartment().getName() : "Bộ môn";
                String reviewer = ref.getReviewerNameSnapshot() != null ? ref.getReviewerNameSnapshot() : "Ban Giám Hiệu";
                sb.append(String.format("Mẫu tham khảo %d [Môn: %s | Người nhận xét: %s]:\n\"%s\"\n\n",
                        refIdx++, sub, reviewer, ref.getContent()));
            }
        }

        return sb.toString().trim();
    }

    public String loadLegalBasis() {
        String path = properties.getLegalBasisPath();
        if (path == null || path.isBlank()) {
            return "(Không có)";
        }
        try {
            Resource res = resourceLoader.getResource(path);
            if (res.exists() && res.isReadable()) {
                try (InputStream is = res.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                    // Strip markdown comments
                    content = content.replaceAll("<!--[\\s\\S]*?-->", "").trim();
                    if (!content.isBlank()) {
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load legal basis from {}: {}", path, e.getMessage());
        }
        return "(Không có)";
    }
}
