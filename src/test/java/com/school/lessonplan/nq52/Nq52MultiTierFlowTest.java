package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52EvaluationForm;
import com.school.lessonplan.nq52.signature.Nq52DigitalSignature;
import com.school.lessonplan.nq52.signature.Nq52SignatureType;
import com.school.lessonplan.nq52.signature.Nq52SigningRole;
import com.school.lessonplan.user.User;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Nq52MultiTierFlowTest {

    @Test
    public void testThreeTierDataIntegrityAndExport() throws Exception {
        Nq52DocxExportService exportService = new Nq52DocxExportService();

        // 1. Tạo Giáo viên và Phiếu đánh giá
        User teacher = new User();
        teacher.setId(10L);
        teacher.setLastName("Nguyễn");
        teacher.setGivenName("Văn An");
        teacher.setEmail("giaovien@school.edu.vn");

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(500L);
        eval.setUser(teacher);
        eval.setEvaluationMonth(9);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setPositionTitle("Giáo viên");
        eval.setDepartmentNameSnapshot("Tổ Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setStatus(Nq52Status.DRAFT);

        // 2. Giáo viên tự chấm
        Nq52EvaluationForm teacherForm = new Nq52EvaluationForm();
        teacherForm.setId(500L);
        teacherForm.setSelectedB1("a"); // 20đ
        teacherForm.setSelectedB2("a"); // 20đ
        teacherForm.setSelectedB3("a"); // 30đ
        teacherForm.setStrengths("Tận tụy, giảng dạy tốt");
        teacherForm.setWeaknesses("Cần ứng dụng CNTT nhiều hơn");
        teacherForm.setSelfClassification("Hoàn thành xuất sắc nhiệm vụ");

        Map<String, Double> selfScores = new HashMap<>();
        // Nhóm A.I.1: 5.0
        selfScores.put("A_I_1_a", 0.75);
        selfScores.put("A_I_1_b", 0.50);
        selfScores.put("A_I_1_c", 0.50);
        selfScores.put("A_I_1_d", 0.75);
        selfScores.put("A_I_1_dd", 0.75);
        selfScores.put("A_I_1_e", 0.75);
        selfScores.put("A_I_1_ee", 0.50);
        selfScores.put("A_I_1_g", 0.50);
        // Nhóm A.I.2: 5.0
        selfScores.put("A_I_2_a", 1.0);
        selfScores.put("A_I_2_b", 1.0);
        selfScores.put("A_I_2_c", 1.0);
        selfScores.put("A_I_2_d", 2.0);
        // Nhóm A.II: 10.0
        selfScores.put("A_II_1_a", 1.0);
        selfScores.put("A_II_1_b", 0.75);
        selfScores.put("A_II_1_c", 0.75);
        selfScores.put("A_II_2_a", 1.5);
        selfScores.put("A_II_2_b", 1.0);
        selfScores.put("A_II_3_a", 1.5);
        selfScores.put("A_II_3_b", 1.0);
        selfScores.put("A_II_4_a", 1.5);
        selfScores.put("A_II_4_b", 1.0);
        // Nhóm A.III: 5.0
        selfScores.put("A_III_1", 2.5);
        selfScores.put("A_III_2", 2.5);
        teacherForm.setSelfScores(selfScores);

        // Giả lập lưu và nộp phiếu
        eval.setSelfScoreA(BigDecimal.valueOf(25.0));
        eval.setSelfScoreB(BigDecimal.valueOf(70.0));
        eval.setSelfTotalScore(BigDecimal.valueOf(95.0));
        eval.setSelfClassification("Hoàn thành xuất sắc nhiệm vụ");
        eval.setStrengths(teacherForm.getStrengths());
        eval.setWeaknesses(teacherForm.getWeaknesses());
        eval.setStatus(Nq52Status.SUBMITTED);

        // 3. Tổ trưởng thẩm định
        User deptLeader = new User();
        deptLeader.setId(20L);
        deptLeader.setLastName("Trần");
        deptLeader.setGivenName("Thị Bình");
        deptLeader.setEmail("totruong@school.edu.vn");

        Nq52EvaluationForm revForm = new Nq52EvaluationForm();
        revForm.setId(500L);
        revForm.setReviewerSelectedB1("a");
        revForm.setReviewerSelectedB2("a");
        revForm.setReviewerSelectedB3("b"); // 25đ -> B = 65đ
        revForm.setReviewerNotes("Đạt tiêu chuẩn, giảng dạy tốt");
        revForm.setReviewerClassification("Hoàn thành tốt nhiệm vụ");

        Map<String, Double> revScores = new HashMap<>(selfScores);
        revScores.put("A_III_2", 2.0); // A = 24.5
        revForm.setReviewerScores(revScores);

        eval.setReviewer(deptLeader);
        eval.setReviewerNameSnapshot("Trần Thị Bình");
        eval.setReviewerScoreA(BigDecimal.valueOf(24.5));
        eval.setReviewerScoreB(BigDecimal.valueOf(65.0));
        eval.setReviewerTotalScore(BigDecimal.valueOf(89.5));
        eval.setReviewerClassification("Hoàn thành tốt nhiệm vụ");
        eval.setReviewerNotes(revForm.getReviewerNotes());
        eval.setStatus(Nq52Status.REVIEWED);

        // Kiểm tra dữ liệu Giáo viên không bị mất khi Tổ trưởng đánh giá
        assertEquals(BigDecimal.valueOf(25.0), eval.getSelfScoreA());
        assertEquals(BigDecimal.valueOf(70.0), eval.getSelfScoreB());
        assertEquals(BigDecimal.valueOf(95.0), eval.getSelfTotalScore());
        assertEquals("Hoàn thành xuất sắc nhiệm vụ", eval.getSelfClassification());

        // 4. Ban Giám Hiệu đánh giá chính thức
        User bgh = new User();
        bgh.setId(1L);
        bgh.setLastName("Ban");
        bgh.setGivenName("Giám Hiệu NVL");
        bgh.setEmail("admin@school.edu.vn");

        Nq52EvaluationForm bghForm = new Nq52EvaluationForm();
        bghForm.setId(500L);
        bghForm.setBghSelectedB1("a");
        bghForm.setBghSelectedB2("a");
        bghForm.setBghSelectedB3("a"); // 30đ -> B = 70đ
        bghForm.setBghNotes("Hội đồng nhất trí xếp loại Xuất sắc");
        bghForm.setBghClassification("Hoàn thành xuất sắc nhiệm vụ");

        Map<String, Double> bghScores = new HashMap<>(selfScores);
        bghForm.setBghScores(bghScores);

        eval.setBghReviewer(bgh);
        eval.setBghReviewerNameSnapshot("Ban Giám Hiệu");
        eval.setBghScoreA(BigDecimal.valueOf(25.0));
        eval.setBghScoreB(BigDecimal.valueOf(70.0));
        eval.setBghTotalScore(BigDecimal.valueOf(95.0));
        eval.setBghClassification("Hoàn thành xuất sắc nhiệm vụ");
        eval.setBghNotes(bghForm.getBghNotes());
        eval.setStatus(Nq52Status.COUNCIL_REVIEWED);

        // Kiểm tra dữ liệu cả 3 cấp đều được bảo toàn
        assertEquals(BigDecimal.valueOf(95.0), eval.getSelfTotalScore());
        assertEquals(BigDecimal.valueOf(89.5), eval.getReviewerTotalScore());
        assertEquals(BigDecimal.valueOf(95.0), eval.getBghTotalScore());

        // 5. Kiểm tra Form tổng hợp 3 cấp
        Nq52EvaluationForm mergedForm = new Nq52EvaluationForm();
        mergedForm.setId(500L);
        mergedForm.setSelectedB1(teacherForm.getSelectedB1());
        mergedForm.setSelectedB2(teacherForm.getSelectedB2());
        mergedForm.setSelectedB3(teacherForm.getSelectedB3());
        mergedForm.setSelfScores(selfScores);
        mergedForm.setSelfClassification(eval.getSelfClassification());

        mergedForm.setReviewerSelectedB1(revForm.getReviewerSelectedB1());
        mergedForm.setReviewerSelectedB2(revForm.getReviewerSelectedB2());
        mergedForm.setReviewerSelectedB3(revForm.getReviewerSelectedB3());
        mergedForm.setReviewerScores(revScores);
        mergedForm.setReviewerClassification(eval.getReviewerClassification());
        mergedForm.setReviewerNotes(eval.getReviewerNotes());

        mergedForm.setBghSelectedB1(bghForm.getBghSelectedB1());
        mergedForm.setBghSelectedB2(bghForm.getBghSelectedB2());
        mergedForm.setBghSelectedB3(bghForm.getBghSelectedB3());
        mergedForm.setBghScores(bghScores);
        mergedForm.setBghClassification(eval.getBghClassification());
        mergedForm.setBghNotes(eval.getBghNotes());

        // 6. Ký số 3 cấp
        List<Nq52DigitalSignature> sigs = new ArrayList<>();
        Nq52DigitalSignature sigTeacher = new Nq52DigitalSignature();
        sigTeacher.setSigningRole(Nq52SigningRole.TEACHER);
        sigTeacher.setSignerNameSnapshot("Nguyễn Văn An");
        sigTeacher.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigTeacher.setCaProvider("VNPT_SMARTCA");
        sigTeacher.setSignedAt(Instant.parse("2026-11-01T08:00:00Z"));
        sigs.add(sigTeacher);

        Nq52DigitalSignature sigDept = new Nq52DigitalSignature();
        sigDept.setSigningRole(Nq52SigningRole.DEPT_LEADER);
        sigDept.setSignerNameSnapshot("Trần Thị Bình");
        sigDept.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigDept.setCaProvider("VNPT_SMARTCA");
        sigDept.setSignedAt(Instant.parse("2026-11-02T10:00:00Z"));
        sigs.add(sigDept);

        Nq52DigitalSignature sigBgh = new Nq52DigitalSignature();
        sigBgh.setSigningRole(Nq52SigningRole.PRINCIPAL);
        sigBgh.setSignerNameSnapshot("TRƯỜNG THCS NGUYỄN VĂN LINH");
        sigBgh.setSignatureType(Nq52SignatureType.USB_TOKEN);
        sigBgh.setCaProvider("BAN_CO_YEU_CHINH_PHU");
        sigBgh.setSignedAt(Instant.parse("2026-11-03T14:00:00Z"));
        sigs.add(sigBgh);

        // 7. Xuất file Word Mẫu 03
        byte[] docx = exportService.generateM03Docx(eval, mergedForm, sigs);
        assertNotNull(docx);
        assertTrue(docx.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            // Kiểm tra Table 5 có dấu X ở ô Hoàn thành xuất sắc nhiệm vụ (Cell 0)
            XWPFTable t5 = doc.getTables().get(5);
            var r0 = t5.getRow(0);
            assertEquals("X", r0.getTableCells().get(0).getParagraphs().get(0).getText().trim(),
                    "Ô 'Hoàn thành xuất sắc nhiệm vụ' phải có dấu X");
            assertEquals("", r0.getTableCells().get(2).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(4).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(6).getParagraphs().get(0).getText().trim());

            // Kiểm tra chữ ký số 3 cấp có đủ ảnh trong tài liệu
            assertTrue(doc.getAllPictures().size() >= 3, "Phải chứa đủ 3 chữ ký số của Giáo viên, Tổ trưởng và BGH");

            // Kiểm tra mục III: Nhận xét của tổ trưởng đã được điền thay thế dòng chấm
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            String docText = sb.toString();
            assertTrue(docText.contains("Đạt tiêu chuẩn, giảng dạy tốt"),
                    "Mục III phải được điền nội dung nhận xét của tổ trưởng");
            assertTrue(docText.contains("* Đề xuất mức xếp loại chất lượng: Hoàn thành tốt nhiệm vụ"),
                    "Đề xuất xếp loại của tổ trưởng phải được điền chính xác");
        }
    }

    @Test
    public void testThreeTierWorkResultFlowAndExcelExport() throws Exception {
        Nq52ExcelExportService excelExportService = new Nq52ExcelExportService();

        // 1. Giáo viên tạo bảng tự đánh giá kết quả (Mẫu 01TKNV)
        User teacher = new User();
        teacher.setId(10L);
        teacher.setLastName("Nguyễn");
        teacher.setGivenName("Văn An");
        teacher.setEmail("giaovien@school.edu.vn");

        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setId(600L);
        wr.setUser(teacher);
        wr.setEvaluationMonth(11);
        wr.setEvaluationYear(2026);
        wr.setTeacherNameSnapshot("Nguyễn Văn An");
        wr.setPositionTitle("Giáo viên Toán");
        wr.setDepartmentNameSnapshot("Tổ Toán học");
        wr.setCampusNameSnapshot("Điểm trường chính");
        wr.setEvaluationDateStr("Cẩm Lệ, ngày 05 tháng 11 năm 2026");
        wr.setStatus(Nq52Status.DRAFT);

        // 2. Giáo viên ký số tự đánh giá -> chuyển sang SUBMITTED
        wr.setStatus(Nq52Status.SUBMITTED);
        wr.setSubmittedAt(Instant.parse("2026-11-05T09:00:00Z"));

        // 3. Tổ trưởng chuyên môn thẩm định -> chuyển sang REVIEWED
        User deptLeader = new User();
        deptLeader.setId(20L);
        deptLeader.setLastName("Trần");
        deptLeader.setGivenName("Thị Bình");
        deptLeader.setEmail("totruong@school.edu.vn");

        wr.setReviewer(deptLeader);
        wr.setReviewerNameSnapshot("Trần Thị Bình");
        wr.setReviewerNotes("Đã thẩm định, các nhiệm vụ thực hiện đảm bảo tiến độ và chất lượng.");
        wr.setStatus(Nq52Status.REVIEWED);
        wr.setReviewedAt(Instant.parse("2026-11-06T10:00:00Z"));

        // 4. Ban Giám Hiệu phê duyệt chính thức -> chuyển sang COUNCIL_REVIEWED
        User bgh = new User();
        bgh.setId(1L);
        bgh.setLastName("Ban");
        bgh.setGivenName("Giám Hiệu NVL");
        bgh.setEmail("admin@school.edu.vn");

        wr.setBghReviewer(bgh);
        wr.setBghReviewerNameSnapshot("Ban Giám Hiệu");
        wr.setBghNotes("Ban Giám Hiệu đồng ý với kết quả tự đánh giá và thẩm định của Tổ chuyên môn.");
        wr.setStatus(Nq52Status.COUNCIL_REVIEWED);
        wr.setBghReviewedAt(Instant.parse("2026-11-07T14:00:00Z"));

        assertEquals(Nq52Status.COUNCIL_REVIEWED, wr.getStatus());
        assertEquals("Trần Thị Bình", wr.getReviewerNameSnapshot());
        assertEquals("Ban Giám Hiệu", wr.getBghReviewerNameSnapshot());
        assertNotNull(wr.getReviewedAt());
        assertNotNull(wr.getBghReviewedAt());

        // 5. Chuẩn bị chữ ký số 3 cấp
        List<Nq52DigitalSignature> sigs = new ArrayList<>();

        Nq52DigitalSignature sigTeacher = new Nq52DigitalSignature();
        sigTeacher.setSigningRole(Nq52SigningRole.TEACHER);
        sigTeacher.setSignerNameSnapshot("Nguyễn Văn An");
        sigTeacher.setCaProvider("VNPT_SMARTCA");
        sigTeacher.setSignedAt(wr.getSubmittedAt());
        sigs.add(sigTeacher);

        Nq52DigitalSignature sigDept = new Nq52DigitalSignature();
        sigDept.setSigningRole(Nq52SigningRole.DEPT_LEADER);
        sigDept.setSignerNameSnapshot("Trần Thị Bình");
        sigDept.setCaProvider("VNPT_SMARTCA");
        sigDept.setSignedAt(wr.getReviewedAt());
        sigs.add(sigDept);

        Nq52DigitalSignature sigBgh = new Nq52DigitalSignature();
        sigBgh.setSigningRole(Nq52SigningRole.PRINCIPAL);
        sigBgh.setSignerNameSnapshot("TRƯỜNG THCS NGUYỄN VĂN LINH");
        sigBgh.setSignatureType(Nq52SignatureType.USB_TOKEN);
        sigBgh.setCaProvider("BAN_CO_YEU_CHINH_PHU");
        sigBgh.setSignedAt(wr.getBghReviewedAt());
        sigs.add(sigBgh);

        // 6. Xuất file Excel Mẫu 01TKNV
        com.school.lessonplan.nq52.dto.Nq52WorkResultForm form = new com.school.lessonplan.nq52.dto.Nq52WorkResultForm();
        form.setId(600L);
        form.setPositionTitle("Giáo viên Toán");
        form.setEvaluationDateStr("Cẩm Lệ, ngày 05 tháng 11 năm 2026");
        form.setTotalAssigned(5);
        form.setTotalCompleted(5);
        form.setTotalUncompleted(0);
        form.setCompletionRate(100.0);
        form.setProgressEarly(1);
        form.setProgressOnTime(4);
        form.setProgressLate(0);
        form.setOnTimeRate(100.0);
        form.setQualityStandard(3);
        form.setQualityExceeded(2);
        form.setQualitySubstandard(0);
        form.setQualityRate(100.0);
        form.setReviewerNotes(wr.getReviewerNotes());
        form.setBghNotes(wr.getBghNotes());

        byte[] excelBytes = excelExportService.generateWorkResultExcel(wr, form, sigs);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "File Excel Mẫu 01TKNV phải được tạo thành công với chữ ký 3 cấp");
    }
}
