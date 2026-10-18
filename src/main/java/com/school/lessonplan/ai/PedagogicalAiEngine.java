package com.school.lessonplan.ai;

import com.school.lessonplan.ai.dto.AiReviewResponse;
import com.school.lessonplan.lessonplan.LessonPlan;
import com.school.lessonplan.lessonplan.LessonPlanVersion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PedagogicalAiEngine {

    public AiReviewResponse analyze(LessonPlan plan, LessonPlanVersion version, String fullText) {
        String lower = (fullText != null) ? fullText.toLowerCase() : "";

        // Trích xuất nội dung văn bản thực tế (bỏ qua phần metadata header)
        String docBody = "";
        int markerIdx = lower.indexOf("--- nội dung văn bản trích xuất ---");
        if (markerIdx >= 0) {
            docBody = lower.substring(markerIdx + "--- nội dung văn bản trích xuất ---".length()).trim();
        } else {
            docBody = lower.trim();
        }

        String subjectName = (plan.getDepartment() != null) ? plan.getDepartment().getName() : "Bộ môn";
        String teacherName = (plan.getOwner() != null) ? plan.getOwner().getFullName() : "Thầy/Cô";

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        String rating;
        String summary;

        // =========================================================================
        // TRƯỜNG HỢP 1: Tệp rỗng, không có dữ liệu văn bản hoặc lỗi đọc tệp
        // =========================================================================
        if (docBody.isEmpty() || docBody.contains("[cảnh báo kỹ thuật: không thể trích xuất") || docBody.length() < 60) {
            rating = "Không đạt yêu cầu (Tệp rỗng hoặc không có dữ liệu bài soạn)";
            summary = "Hệ thống không tìm thấy nội dung bài dạy trong tệp đính kèm (" + version.getOriginalFilename() + "). Tệp có thể rỗng, bị hỏng hoặc thuộc định dạng quét ảnh.";
            
            improvements.add("❌ [LỖI TỆP ĐÍNH KÈM] Tệp đính kèm không có nội dung văn bản kế hoạch bài dạy.");
            improvements.add("🔍 [KIỂM TRA CỦA BGH] BGH cần yêu cầu giáo viên nộp lại tệp .docx chuẩn có đầy đủ nội dung bài soạn để tiến hành thẩm định chuyên môn.");

            StringBuilder draft = new StringBuilder();
            draft.append("Kính gửi Thầy/Cô ").append(teacherName).append(",\n\n");
            draft.append("Ban Giám Hiệu đã kiểm tra tệp kế hoạch bài dạy \"").append(plan.getTitle()).append("\" (").append(subjectName).append(" - Khối ").append(plan.getGrade()).append(").\n\n");
            draft.append("KẾT LUẬN: KHÔNG DUYỆT phiên bản này do tệp đính kèm không có dữ liệu văn bản hoặc tệp bị rỗng/lỗi định dạng. Đề nghị Thầy/Cô kiểm tra lại tệp văn bản và nộp lại bản chuẩn trong vòng 24 giờ.");

            return buildResponse(rating, summary, strengths, improvements, draft.toString());
        }

        // =========================================================================
        // TRƯỜNG HỢP 2: Tệp có chữ nhưng KHÔNG PHẢI KẾ HOẠCH BÀI DẠY (Tệp lạc đề)
        // =========================================================================
        int pedagogicalAnchors = countMatches(docBody,
                "mục tiêu", "yêu cầu cần đạt", "khởi động", "mở đầu", "khám phá",
                "hình thành kiến thức", "luyện tập", "vận dụng", "tiến trình",
                "hoạt động", "thiết bị dạy học", "học liệu", "kế hoạch bài dạy",
                "giáo án", "nhiệm vụ", "sản phẩm học tập");

        if (pedagogicalAnchors < 2) {
            rating = "Không đạt yêu cầu (Tệp không phải giáo án)";
            summary = "Nội dung tệp hoàn toàn không phù hợp với cấu trúc Kế hoạch bài dạy theo quy định của Bộ GD&ĐT (thiếu toàn bộ cấu trúc sư phạm cơ bản).";
            
            improvements.add("❌ [SAI NỘI DUNG TỆP] Văn bản không có các đề mục bắt buộc của Kế hoạch bài dạy (không có Mục tiêu, không có Chuỗi 4 hoạt động học).");
            improvements.add("🔍 [YÊU CẦU BGH] BGH cần kiểm tra trực tiếp: Giáo viên có thể đã tải nhầm tệp đề cương, báo cáo hành chính, danh sách điểm hoặc tài liệu cá nhân thay vì Kế hoạch bài dạy.");

            StringBuilder draft = new StringBuilder();
            draft.append("Kính gửi Thầy/Cô ").append(teacherName).append(",\n\n");
            draft.append("Qua kiểm tra kỹ thuật kế hoạch bài dạy \"").append(plan.getTitle()).append("\", Ban Giám Hiệu KHÔNG DUYỆT tệp đính kèm hiện tại vì văn bản không phải là kế hoạch bài dạy theo quy chuẩn của Bộ GD&ĐT.\n\n");
            draft.append("Đề nghị Thầy/Cô thu hồi tệp này và nộp đúng kế hoạch bài dạy theo quy định tại Thông tư 15/2026/TT-BGDĐT và Thông tư 17/2025/TT-BGDĐT trước khi lên lớp.");

            return buildResponse(rating, summary, strengths, improvements, draft.toString());
        }

        // =========================================================================
        // TRƯỜNG HỢP 3: LÀ GIÁO ÁN -> THẨM TRA KỸ THUẬT CHI TIẾT (TT 15/2026 & TT 17/2025)
        // =========================================================================

        // 1. Kiểm tra Mục I: Mục tiêu theo TT 17/2025 (Kiến thức, Năng lực chung/đặc thù, Phẩm chất, Năng lực số/STEM)
        boolean hasNangLucChung = containsAny(docBody, "tự chủ", "tự học", "giao tiếp", "hợp tác", "giải quyết vấn đề", "sáng tạo");
        boolean hasNangLucDacThu = containsAny(docBody, "năng lực đặc thù", "năng lực ngôn ngữ", "năng lực toán", "năng lực tin", "năng lực công nghệ", "năng lực khoa học", "năng lực thể chất", "năng lực thẩm mỹ", "năng lực nhận thức");
        boolean hasPhamChat = containsAny(docBody, "phẩm chất", "chăm chỉ", "trách nhiệm", "trung thực", "nhân ái", "yêu nước");
        boolean hasNangLucSoOrStem = containsAny(docBody, "năng lực số", "công nghệ số", "chuyển đổi số", "tin học", "stem", "steam", "thực tiễn", "trải nghiệm");
        boolean hasMucTieuTongThe = containsAny(docBody, "mục tiêu", "yêu cầu cần đạt");

        // 2. Kiểm tra Mục II: Thiết bị dạy học và học liệu số theo TT 15/2026
        boolean hasHocLieuSo = containsAny(docBody, "học liệu số", "bài giảng điện tử", "cntt", "powerpoint", "video", "trực tuyến", "thiết bị số", "link", "padlet", "quizizz", "canva");
        boolean hasHocLieu = hasHocLieuSo || containsAny(docBody, "thiết bị dạy học", "học liệu", "đồ dùng dạy học", "phiếu học tập", "máy chiếu");
        boolean hasPhanDinhGvHs = (containsAny(docBody, "giáo viên:", "gv:", "thầy cô:") && containsAny(docBody, "học sinh:", "hs:", "trò:"));

        // 3. Kiểm tra Mục III: Chuỗi 4 hoạt động học tập thực chất (Kế thừa CV 5512 & TT 15/2026)
        boolean hasKhoiDong = containsAny(docBody, "khởi động", "mở đầu", "xác định vấn đề", "tạo tình huống", "tình huống xuất phát");
        boolean hasKhamPha = containsAny(docBody, "hình thành kiến thức", "khám phá", "tìm hiểu kiến thức", "nội dung bài");
        boolean hasLuyenTap = containsAny(docBody, "luyện tập", "thực hành", "bài tập", "củng cố");
        boolean hasVanDung = containsAny(docBody, "vận dụng", "mở rộng", "liên hệ thực tế", "tìm tòi mở rộng");
        int activityCount = (hasKhoiDong ? 1 : 0) + (hasKhamPha ? 1 : 0) + (hasLuyenTap ? 1 : 0) + (hasVanDung ? 1 : 0);

        // 4. Kiểm tra Kỹ thuật 4 bước tổ chức trong từng hoạt động
        boolean hasChuyenGiao = containsAny(docBody, "chuyển giao", "giao nhiệm vụ", "bước 1");
        boolean hasThucHien = containsAny(docBody, "thực hiện nhiệm vụ", "hs làm việc", "tiến hành", "bước 2");
        boolean hasBaoCao = containsAny(docBody, "báo cáo", "thảo luận", "chia sẻ", "trình bày", "bước 3");
        boolean hasKetLuan = containsAny(docBody, "kết luận", "nhận định", "chốt kiến thức", "chuẩn hóa", "bước 4");
        boolean hasDu4BuocToChuc = hasChuyenGiao && hasThucHien && hasBaoCao && hasKetLuan;

        // 5. Kiểm tra Sản phẩm học tập dự kiến
        boolean hasSanPham = containsAny(docBody, "sản phẩm", "sản phẩm học tập", "dự kiến sản phẩm", "kết quả thực hiện");

        // 6. Kiểm tra Dự kiến thời lượng (phân bổ số phút theo tinh thần tinh giản TT 15/2026)
        boolean hasThoiLuong = Pattern.compile("(?i)\\d+\\s*phút").matcher(docBody).find() || containsAny(docBody, "thời lượng:", "dự kiến:");

        // 7. Kiểm tra Đổi mới kiểm tra đánh giá thường xuyên (TT 17/2025)
        boolean hasDanhGiaThuongXuyen = containsAny(docBody, "rubric", "bảng kiểm", "thang đo", "tự đánh giá", "đánh giá đồng đẳng", "đánh giá thường xuyên", "phiếu học tập");

        // 8. Phương pháp dạy học tích cực
        boolean hasPhuongPhap = containsAny(docBody, "thảo luận nhóm", "dạy học tích cực", "trực quan", "khăn trải bàn", "kỹ thuật mảnh ghép", "động não", "bể cá", "dự án");

        // === ĐÁNH GIÁ ƯU ĐIỂM (CHỈ GHI NHẬN NHỮNG GÌ THỰC SỰ CÓ) ===
        if (activityCount >= 4) {
            strengths.add("✔️ Thiết kế đầy đủ chuỗi 4 hoạt động học thực chất, không hình thức theo tinh thần đổi mới của Thông tư 15/2026 (Khởi động -> Khám phá -> Luyện tập -> Vận dụng).");
        } else if (activityCount >= 3) {
            strengths.add("✔️ Thiết kế được các hoạt động học tập chính, bám sát tiến trình hình thành kiến thức trọng tâm của bài.");
        }

        if (hasDu4BuocToChuc) {
            strengths.add("✔️ Thể hiện rõ 4 bước kỹ thuật tổ chức sư phạm trong mỗi hoạt động (Chuyển giao NV -> Thực hiện NV -> Báo cáo, thảo luận -> Kết luận, nhận định).");
        }

        if (hasNangLucChung && hasPhamChat) {
            strengths.add("✔️ Mục tiêu bài dạy bám sát Chương trình GDPT 2018 điều chỉnh theo Thông tư 17/2025/TT-BGDĐT, nêu rõ các năng lực và phẩm chất cần hình thành cho học sinh.");
        }

        if (hasNangLucSoOrStem) {
            strengths.add("✔️ Tích hợp tốt định hướng phát triển Năng lực số / giáo dục STEM và giải quyết vấn đề thực tiễn theo đúng tinh thần Thông tư 17/2025.");
        }

        if (hasHocLieuSo) {
            strengths.add("✔️ Đẩy mạnh ứng dụng học liệu số, thiết bị CNTT vào tổ chức dạy học theo đúng định hướng chuyển đổi số của Thông tư 15/2026/TT-BGDĐT.");
        } else if (hasHocLieu) {
            strengths.add("✔️ Chuẩn bị thiết bị và học liệu dạy học trực quan, hỗ trợ học sinh học tập tích cực.");
        }

        if (hasDanhGiaThuongXuyen) {
            strengths.add("✔️ Có công cụ kiểm tra đánh giá thường xuyên (phiếu học tập, rubric, bảng kiểm) hỗ trợ đánh giá quá trình theo Thông tư 17/2025.");
        }

        // === BẮT LỖI KỸ THUẬT CỤ THỂ CHO BAN GIÁM HIỆU ĐẾN CHECK GIÁO VIÊN ===
        int majorFlaws = 0;

        // Lỗi 1: Cấu trúc 4 hoạt động
        if (!hasKhoiDong) {
            improvements.add("❌ [TIẾN TRÌNH - THIẾU HĐ KHỞI ĐỘNG] Thiếu Hoạt động 1 (Khởi động/Xác định vấn đề) để kích hoạt kiến thức nền và tạo tình huống xuất phát.");
            majorFlaws++;
        }
        if (!hasKhamPha) {
            improvements.add("❌ [TIẾN TRÌNH - THIẾU HĐ HÌNH THÀNH KIẾN THỨC] Thiếu Hoạt động 2 (Hình thành kiến thức mới/Khám phá) theo chuẩn cấu trúc.");
            majorFlaws++;
        }
        if (!hasLuyenTap) {
            improvements.add("❌ [TIẾN TRÌNH - THIẾU HĐ LUYỆN TẬP] Thiếu Hoạt động 3 (Luyện tập/Thực hành) để học sinh củng cố và giải quyết bài tập trực tiếp.");
            majorFlaws++;
        }
        if (!hasVanDung) {
            improvements.add("❌ [TIẾN TRÌNH - THIẾU HĐ VẬN DỤNG] Thiếu Hoạt động 4 (Vận dụng thực tiễn). Theo TT 17/2025, hoạt động này cần gắn với thực tế đời sống, không thay thế bằng dặn dò bài tập về nhà.");
            majorFlaws++;
        }

        // Lỗi 2: Kỹ thuật 4 bước tổ chức hoạt động
        if (!hasDu4BuocToChuc) {
            improvements.add("❌ [KỸ THUẬT TỔ CHỨC HOẠT ĐỘNG] Các hoạt động chưa thể hiện đủ 4 bước chuẩn (1. Chuyển giao nhiệm vụ -> 2. Thực hiện nhiệm vụ -> 3. Báo cáo, thảo luận -> 4. Kết luận, nhận định). Cần tránh lối soạn độc thoại một chiều của giáo viên.");
            majorFlaws++;
        }

        // Lỗi 3: Mục tiêu theo TT 17/2025
        if (!hasNangLucChung && !hasNangLucDacThu) {
            improvements.add("❌ [MỤC TIÊU - NĂNG LỰC] Mục tiêu chưa bám sát YCCĐ của TT 17/2025, chưa phân tách rõ Năng lực chung (tự chủ, giao tiếp, hợp tác) và Năng lực đặc thù của môn học.");
            majorFlaws++;
        }
        if (!hasPhamChat) {
            improvements.add("❌ [MỤC TIÊU - PHẨM CHẤT] Thiếu mục tiêu bồi dưỡng Phẩm chất (yêu nước, nhân ái, chăm chỉ, trung thực, trách nhiệm) gắn với các hành vi cụ thể trong bài.");
        }
        if (!hasNangLucSoOrStem) {
            improvements.add("⚠️ [ĐỊNH HƯỚNG TT 17/2025] Nên bổ sung tích hợp Năng lực số hoặc yếu tố thực hành / liên hệ thực tế (STEM) phù hợp với nội dung bài học.");
        }

        // Lỗi 4: Sản phẩm học tập
        if (!hasSanPham) {
            improvements.add("❌ [SẢN PHẨM HỌC TẬP] Chưa nêu rõ Sản phẩm học tập dự kiến của học sinh ở từng hoạt động (cần ghi cụ thể câu trả lời, phiếu học tập đã điền, hoặc bài giải thay vì ghi chung chung).");
            majorFlaws++;
        }

        // Lỗi 5: Phân bổ thời lượng theo TT 15/2026
        if (!hasThoiLuong) {
            improvements.add("❌ [PHÂN BỔ THỜI GIAN] Chưa dự kiến số phút cụ thể cho từng hoạt động để BGH kiểm soát tính khả thi trong tiết dạy 45 phút theo quy định TT 15/2026.");
        }

        // Lỗi 6: Thiết bị & Học liệu số theo TT 15/2026
        if (!hasHocLieuSo) {
            improvements.add("⚠️ [HỌC LIỆU SỐ - TT 15/2026] Khuyến khích tích hợp học liệu số, video clip, bài giảng điện tử hoặc phiếu học tập số hóa để tăng tính tương tác.");
        }

        // Lỗi 7: Đánh giá thường xuyên theo TT 17/2025
        if (!hasDanhGiaThuongXuyen) {
            improvements.add("⚠️ [ĐÁNH GIÁ THƯỜNG XUYÊN - TT 17/2025] Nên đính kèm Phiếu học tập hoặc Công cụ đánh giá quá trình (Rubric/Bảng kiểm/Đánh giá đồng đẳng) cho các hoạt động.");
        }

        // === XẾP LOẠI CHÍNH XÁC DỰA TRÊN SỐ LỖI KỸ THUẬT ===
        if (majorFlaws >= 3) {
            rating = "Chưa đạt yêu cầu (Cần chỉnh sửa theo TT 15/2026 & TT 17/2025)";
            summary = "Giáo án còn thiếu nhiều tiêu chuẩn kỹ thuật sư phạm của Thông tư 15/2026/TT-BGDĐT và Thông tư 17/2025/TT-BGDĐT. Cần bổ sung và chỉnh sửa trước khi triển khai giảng dạy.";
        } else if (majorFlaws >= 1 || activityCount < 4) {
            rating = "Đạt yêu cầu (Cần hoàn thiện một số mục)";
            summary = "Kế hoạch bài dạy đáp ứng cấu trúc khung cơ bản, nhưng cần khắc phục một số điểm kỹ thuật sư phạm cụ thể trước khi Ban Giám Hiệu duyệt chính thức.";
        } else if (activityCount >= 4 && hasDu4BuocToChuc && hasNangLucChung && hasPhamChat) {
            rating = "Tốt (Đạt chuẩn TT 15/2026 & TT 17/2025)";
            summary = "Kế hoạch bài dạy chuẩn mực, bám sát tinh thần đổi mới thực chất của Thông tư 15/2026/TT-BGDĐT và Chương trình GDPT 2018 điều chỉnh theo Thông tư 17/2025/TT-BGDĐT.";
        } else {
            rating = "Đạt yêu cầu (Cần tinh chỉnh)";
            summary = "Kế hoạch bài dạy đảm bảo yêu cầu giảng dạy, cần rà soát lại phân bổ thời gian và công cụ đánh giá thường xuyên.";
        }

        // === BIÊN BẢN NHẬN XÉT CỦA BAN GIÁM HIỆU ===
        StringBuilder draft = new StringBuilder();
        draft.append("Kính gửi Thầy/Cô ").append(teacherName).append(",\n\n");
        draft.append("Ban Giám Hiệu đã tiến hành thẩm tra Kỹ thuật Kế hoạch bài dạy \"").append(plan.getTitle()).append("\" (").append(subjectName).append(" - Khối ").append(plan.getGrade()).append(") căn cứ theo quy định tại Thông tư 15/2026/TT-BGDĐT và Thông tư 17/2025/TT-BGDĐT. Xin gửi thông báo kiểm tra chuyên môn như sau:\n\n");

        draft.append("1. CÁC ĐIỂM KỸ THUẬT ĐẠT YÊU CẦU:\n");
        if (strengths.isEmpty()) {
            draft.append(" - Cần hoàn thiện lại cấu trúc theo hướng dẫn của Thông tư 15/2026 và Thông tư 17/2025.\n");
        } else {
            for (String s : strengths) {
                draft.append(" - ").append(s).append("\n");
            }
        }

        draft.append("\n2. CÁC ĐIỂM KỸ THUẬT CẦN ĐIỀU CHỈNH (BGH SẼ TRỰC TIẾP KIỂM TRA):\n");
        if (improvements.isEmpty()) {
            draft.append(" - Kế hoạch bài dạy hoàn thiện chu đáo, bám sát yêu cầu mới, có thể triển khai giảng dạy.\n");
        } else {
            for (String imp : improvements) {
                draft.append(" - ").append(imp).append("\n");
            }
        }

        draft.append("\n3. KẾT LUẬN CỦA BAN GIÁM HIỆU:\n");
        draft.append("Kế hoạch bài dạy xếp loại: ").append(rating).append(".\n");
        if (majorFlaws >= 3) {
            draft.append("Đề nghị Thầy/Cô khẩn trương điều chỉnh các lỗi kỹ thuật đã nêu ở mục 2 và nộp lại phiên bản sửa đổi cho Tổ trưởng chuyên môn và BGH trước ngày lên lớp. BGH sẽ kiểm tra thực tế việc triển khai các hoạt động này khi dự giờ.");
        } else {
            draft.append("Thầy/Cô lưu ý hoàn thiện các điểm góp ý trên để nâng cao chất lượng giờ dạy theo định hướng đổi mới. Chúc Thầy/Cô có tiết dạy thành công!");
        }

        return buildResponse(rating, summary, strengths, improvements, draft.toString());
    }

    private AiReviewResponse buildResponse(String rating, String summary, List<String> strengths, List<String> improvements, String feedback) {
        AiReviewResponse response = new AiReviewResponse();
        response.setSuccess(true);
        response.setSource("PEDAGOGICAL_ENGINE");
        response.setModelName("Bộ thẩm định Chuẩn Sư phạm (TT 15/2026 & TT 17/2025/TT-BGDĐT)");
        response.setOverallRating(rating);
        response.setSummary(summary);
        response.setStrengths(strengths);
        response.setImprovements(improvements);
        response.setSuggestedFeedback(feedback);
        return response;
    }

    private int countMatches(String text, String... keywords) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) count++;
        }
        return count;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
