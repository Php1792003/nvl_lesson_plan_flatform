package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52EvaluationForm;
import com.school.lessonplan.nq52.signature.Nq52DigitalSignature;
import com.school.lessonplan.nq52.signature.Nq52SignatureType;
import com.school.lessonplan.nq52.signature.Nq52SigningRole;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Nq52DocxExportServiceTest {

    @Test
    public void testGenerateM03DocxWithSignatures() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(100L);
        eval.setEvaluationMonth(11);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setPositionTitle("Giáo viên Toán");
        eval.setDepartmentNameSnapshot("Tổ Toán");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(100L);
        form.setMonth(11);
        form.setYear(2026);
        form.setPositionTitle("Giáo viên Toán");
        form.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        List<Nq52DigitalSignature> signatures = new ArrayList<>();

        Nq52DigitalSignature sigTeacher = new Nq52DigitalSignature();
        sigTeacher.setSigningRole(Nq52SigningRole.TEACHER);
        sigTeacher.setSignerNameSnapshot("Nguyễn Văn An");
        sigTeacher.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigTeacher.setCaProvider("VNPT_SMARTCA");
        sigTeacher.setSignedAt(Instant.parse("2026-11-01T10:15:00Z"));
        signatures.add(sigTeacher);

        Nq52DigitalSignature sigDept = new Nq52DigitalSignature();
        sigDept.setSigningRole(Nq52SigningRole.DEPT_LEADER);
        sigDept.setSignerNameSnapshot("Trần Thị Bình");
        sigDept.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigDept.setCaProvider("VNPT_SMARTCA");
        sigDept.setSignedAt(Instant.parse("2026-11-02T14:20:00Z"));
        signatures.add(sigDept);

        Nq52DigitalSignature sigBgh = new Nq52DigitalSignature();
        sigBgh.setSigningRole(Nq52SigningRole.PRINCIPAL);
        sigBgh.setSignerNameSnapshot("TRƯỜNG THCS NGUYỄN VĂN LINH");
        sigBgh.setSignatureType(Nq52SignatureType.USB_TOKEN);
        sigBgh.setCaProvider("BAN_CO_YEU_CHINH_PHU");
        sigBgh.setSignedAt(Instant.parse("2026-11-02T16:00:00Z"));
        signatures.add(sigBgh);

        byte[] docxBytes = service.generateM03Docx(eval, form, signatures);
        assertNotNull(docxBytes);
        assertTrue(docxBytes.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            String fullText = sb.toString();

            assertTrue(doc.getAllPictures().size() >= 3, "Docx must contain 3 digital signature picture stamps");
            assertTrue(fullText.contains("Nguyễn Văn An"), "Docx must contain teacher signer name");
            assertTrue(fullText.contains("Trần Thị Bình"), "Docx must contain dept leader signer name");
            assertTrue(fullText.contains("Ban Giám Hiệu"), "Docx must contain BGH title");

            // Kiểm tra vị trí chữ ký giáo viên: P[40] là ảnh, P[41] là họ và tên
            XWPFParagraph p40 = doc.getParagraphs().get(40);
            XWPFParagraph p41 = doc.getParagraphs().get(41);
            assertEquals("Nguyễn Văn An", p41.getText().trim(), "P[41] phải chứa họ tên người tự đánh giá ngay dưới ảnh");
            assertFalse(p40.getRuns().isEmpty(), "P[40] phải chứa run hình ảnh");
            assertFalse(p40.getRuns().get(0).getEmbeddedPictures().isEmpty(), "P[40] phải có embedded picture chữ ký");
        }
    }

    @Test
    public void testClassificationCheckboxWithScoreFallback() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(101L);
        eval.setEvaluationMonth(11);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Trần Văn C");
        eval.setDepartmentNameSnapshot("Tổ Sử - Địa");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        // Không set selfClassification, chỉ set điểm 85 -> Tốt (Cell 2)
        eval.setSelfTotalScore(BigDecimal.valueOf(85.0));

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(101L);
        form.setMonth(11);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            assertTrue(doc.getTables().size() >= 6, "Doc must have Table 5");
            var t5 = doc.getTables().get(5);
            var r0 = t5.getRow(0);

            // Cell 0 (Xuất sắc) không có X
            assertEquals("", r0.getTableCells().get(0).getParagraphs().get(0).getText().trim());
            // Cell 2 (Tốt) có dấu X
            assertEquals("X", r0.getTableCells().get(2).getParagraphs().get(0).getText().trim());
            // Cell 4 (Hoàn thành) không có X
            assertEquals("", r0.getTableCells().get(4).getParagraphs().get(0).getText().trim());
            // Cell 6 (Không hoàn thành) không có X
            assertEquals("", r0.getTableCells().get(6).getParagraphs().get(0).getText().trim());
        }
    }

    @Test
    public void testClassificationCheckboxExplicitExcellent() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(102L);
        eval.setEvaluationMonth(11);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Lê Thị D");
        eval.setDepartmentNameSnapshot("Tổ Văn");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setSelfClassification("Hoàn thành xuất sắc nhiệm vụ");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(102L);
        form.setMonth(11);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            var t5 = doc.getTables().get(5);
            var r0 = t5.getRow(0);

            // Cell 0 (Xuất sắc) phải có dấu X
            assertEquals("X", r0.getTableCells().get(0).getParagraphs().get(0).getText().trim());
            // Cell 2, 4, 6 không có X
            assertEquals("", r0.getTableCells().get(2).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(4).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(6).getParagraphs().get(0).getText().trim());
        }
    }

    @Test
    public void testClassificationCheckboxCompletedTask() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(103L);
        eval.setEvaluationMonth(10);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setSelfClassification("Hoàn thành nhiệm vụ");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(103L);
        form.setMonth(10);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            var t5 = doc.getTables().get(5);
            var r0 = t5.getRow(0);

            // Cell 4 (Hoàn thành nhiệm vụ) phải có dấu X
            assertEquals("X", r0.getTableCells().get(4).getParagraphs().get(0).getText().trim());
            // Cell 0, 2, 6 không có X
            assertEquals("", r0.getTableCells().get(0).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(2).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(6).getParagraphs().get(0).getText().trim());
        }
    }

    @Test
    public void testClassificationCheckboxZeroScoreDefaultsToCompletedTask() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(104L);
        eval.setEvaluationMonth(10);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setSelfTotalScore(BigDecimal.ZERO);
        eval.setSelfClassification(null);

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(104L);
        form.setMonth(10);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            var t5 = doc.getTables().get(5);
            var r0 = t5.getRow(0);

            // Khi điểm = 0 và chưa xếp loại, mặc định mức tự đề xuất là "Hoàn thành nhiệm vụ" (Cell 4)
            assertEquals("", r0.getTableCells().get(0).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(2).getParagraphs().get(0).getText().trim());
            assertEquals("X", r0.getTableCells().get(4).getParagraphs().get(0).getText().trim());
            assertEquals("", r0.getTableCells().get(6).getParagraphs().get(0).getText().trim());
        }
    }

    @Test
    public void testReviewerNotesFilledInDocx() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(105L);
        eval.setEvaluationMonth(10);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setReviewerNotes("Đồng chí Nguyễn Văn An luôn gương mẫu, hoàn thành xuất sắc nhiệm vụ chuyên môn.");
        eval.setReviewerClassification("Hoàn thành xuất sắc nhiệm vụ");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(105L);
        form.setMonth(10);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            boolean foundNotes = false;
            boolean foundClassification = false;
            boolean foundDottedInReviewSection = false;

            int sectionHeadingIdx = -1;
            for (int i = 0; i < doc.getParagraphs().size(); i++) {
                String t = doc.getParagraphs().get(i).getText();
                if (t.contains("NHẬN XÉT, ĐÁNH GIÁ") && t.contains("ĐƠN VỊ TRỰC TIẾP SỬ DỤNG")) {
                    sectionHeadingIdx = i;
                    break;
                }
            }

            assertTrue(sectionHeadingIdx >= 0, "Docx must contain reviewer section heading");

            // Kiểm tra các paragraph nằm giữa tiêu đề mục III và dòng đề xuất mức xếp loại
            for (int j = sectionHeadingIdx + 1; j < doc.getParagraphs().size(); j++) {
                String t = doc.getParagraphs().get(j).getText();
                if (t.contains("Đề xuất mức xếp loại")) {
                    if (t.contains("Hoàn thành xuất sắc nhiệm vụ")) {
                        foundClassification = true;
                    }
                    break;
                }
                if (t.contains("Đồng chí Nguyễn Văn An luôn gương mẫu, hoàn thành xuất sắc nhiệm vụ chuyên môn.")) {
                    foundNotes = true;
                }
                if (t.contains("..................................................")) {
                    foundDottedInReviewSection = true;
                }
            }

            assertTrue(foundNotes, "Mục III phải được điền nội dung nhận xét của tổ trưởng thay cho dòng chấm");
            assertTrue(foundClassification, "Dòng đề xuất mức xếp loại phải được điền xếp loại của tổ trưởng");
            assertFalse(foundDottedInReviewSection, "Không được còn dòng chấm dài ở phần nhận xét khi đã có dữ liệu");
        }
    }

    @Test
    public void testReviewerNotesMultiLineInDocx() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(106L);
        eval.setEvaluationMonth(10);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setReviewerNotes("- Hoàn thành tốt giảng dạy môn Toán khối 9.\n- Tích cực bồi dưỡng học sinh giỏi đạt kết quả tốt.\n- Thực hiện nghiêm túc quy chế của nhà trường.");
        eval.setReviewerClassification("Hoàn thành tốt nhiệm vụ");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(106L);
        form.setMonth(10);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder allText = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                allText.append(p.getText()).append("\n");
            }
            String full = allText.toString();

            assertTrue(full.contains("Hoàn thành tốt giảng dạy môn Toán khối 9."), "Chứa dòng 1 của nhận xét");
            assertTrue(full.contains("Tích cực bồi dưỡng học sinh giỏi đạt kết quả tốt."), "Chứa dòng 2 của nhận xét");
            assertTrue(full.contains("Thực hiện nghiêm túc quy chế của nhà trường."), "Chứa dòng 3 của nhận xét");
            assertTrue(full.contains("* Đề xuất mức xếp loại chất lượng: Hoàn thành tốt nhiệm vụ"), "Đề xuất xếp loại tốt");
        }
    }

    @Test
    public void testReviewerNotesBlankPreservesTemplateDots() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(107L);
        eval.setEvaluationMonth(10);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setReviewerNotes(null);
        eval.setReviewerClassification(null);

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(107L);
        form.setMonth(10);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            int sectionHeadingIdx = -1;
            for (int i = 0; i < doc.getParagraphs().size(); i++) {
                String t = doc.getParagraphs().get(i).getText();
                if (t.contains("NHẬN XÉT, ĐÁNH GIÁ") && t.contains("ĐƠN VỊ TRỰC TIẾP SỬ DỤNG")) {
                    sectionHeadingIdx = i;
                    break;
                }
            }

            assertTrue(sectionHeadingIdx >= 0);
            boolean hasDots = false;
            for (int j = sectionHeadingIdx + 1; j < doc.getParagraphs().size(); j++) {
                String t = doc.getParagraphs().get(j).getText();
                if (t.contains("Đề xuất mức xếp loại")) break;
                if (t.contains("..................................................")) {
                    hasDots = true;
                }
            }
            assertTrue(hasDots, "Khi chưa có nhận xét, các dòng chấm phải được bảo lưu");
        }
    }

    @Test
    public void testSignDocxOnNov04MatchesExactExecutionTime() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(108L);
        eval.setEvaluationMonth(11);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(108L);
        form.setMonth(11);
        form.setYear(2026);
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        Instant signedNov04 = Instant.parse("2026-11-04T00:00:00Z"); // 07:00:00 VN Time

        Nq52DigitalSignature sigTeacher = new Nq52DigitalSignature();
        sigTeacher.setSigningRole(Nq52SigningRole.TEACHER);
        sigTeacher.setSignerNameSnapshot("Nguyễn Văn An");
        sigTeacher.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigTeacher.setCaProvider("VNPT_SMARTCA");
        sigTeacher.setSignedAt(signedNov04);

        Nq52DigitalSignature sigDept = new Nq52DigitalSignature();
        sigDept.setSigningRole(Nq52SigningRole.DEPT_LEADER);
        sigDept.setSignerNameSnapshot("Trần Thị Bình");
        sigDept.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigDept.setCaProvider("VNPT_SMARTCA");
        sigDept.setSignedAt(signedNov04.plusSeconds(3600));

        Nq52DigitalSignature sigBgh = new Nq52DigitalSignature();
        sigBgh.setSigningRole(Nq52SigningRole.PRINCIPAL);
        sigBgh.setSignerNameSnapshot("TRƯỜNG THCS NGUYỄN VĂN LINH");
        sigBgh.setSignatureType(Nq52SignatureType.USB_TOKEN);
        sigBgh.setCaProvider("BAN_CO_YEU_CHINH_PHU");
        sigBgh.setSignedAt(signedNov04.plusSeconds(7200));

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of(sigTeacher, sigDept, sigBgh));
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<String> dateLines = new ArrayList<>();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText().trim();
                if (t.contains("Cẩm Lệ, ngày")) {
                    dateLines.add(t);
                }
            }

            assertTrue(dateLines.size() >= 3, "Phải tìm thấy ít nhất 3 dòng ngày tháng trong văn bản");
            for (String dl : dateLines) {
                assertTrue(dl.contains("ngày 04 tháng 11 năm 2026"),
                        "Dòng ngày ký trong file Word phải đúng ngày 04 tháng 11 năm 2026, thực tế: " + dl);
            }
        }
    }

    @Test
    public void testBghClassificationFilledInDocx() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(109L);
        eval.setEvaluationMonth(11);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Trần Đình Quân");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        eval.setBghClassification("Hoàn thành xuất sắc nhiệm vụ");
        eval.setBghNotes("Đồng chí luôn nêu cao tinh thần trách nhiệm, hoàn thành xuất sắc mọi nhiệm vụ.");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(109L);
        form.setMonth(11);
        form.setYear(2026);
        form.setBghClassification("Hoàn thành xuất sắc nhiệm vụ");
        form.setBghNotes("Đồng chí luôn nêu cao tinh thần trách nhiệm, hoàn thành xuất sắc mọi nhiệm vụ.");
        form.setSelfScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            boolean foundBghClass = false;
            boolean foundBghHeader = false;

            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText().trim();
                if (t.contains("Xếp loại:") && t.contains("Hoàn thành xuất sắc nhiệm vụ")) {
                    foundBghClass = true;
                }
                if (t.contains("KẾT LUẬN CỦA CƠ QUAN CÓ THẨM QUYỀN")) {
                    foundBghHeader = true;
                }
            }

            assertTrue(foundBghHeader, "Trong file Word phải có tiêu đề KẾT LUẬN CỦA CƠ QUAN CÓ THẨM QUYỀN");
            assertTrue(foundBghClass, "Trong file Word phải điền chính xác 'Xếp loại: Hoàn thành xuất sắc nhiệm vụ' theo dữ liệu BGH nhập");
        }
    }

    @Test
    public void testClassificationCheckboxFixedToTeacherEvaluationWhenReviewerAndBghChangeScoresAndClassification() throws Exception {
        Nq52DocxExportService service = new Nq52DocxExportService();

        Nq52Evaluation eval = new Nq52Evaluation();
        eval.setId(110L);
        eval.setEvaluationMonth(11);
        eval.setEvaluationYear(2026);
        eval.setTeacherNameSnapshot("Nguyễn Văn An");
        eval.setDepartmentNameSnapshot("Toán học");
        eval.setWorkUnit("Trường THCS Nguyễn Văn Linh");

        // 1. Giáo viên tự đánh giá: Hoàn thành tốt nhiệm vụ (điểm 85.0)
        eval.setSelfTotalScore(BigDecimal.valueOf(85.0));
        eval.setSelfClassification("Hoàn thành tốt nhiệm vụ");

        // 2. Tổ trưởng đánh giá nâng lên: Hoàn thành xuất sắc nhiệm vụ (điểm 96.0)
        eval.setReviewerTotalScore(BigDecimal.valueOf(96.0));
        eval.setReviewerClassification("Hoàn thành xuất sắc nhiệm vụ");
        eval.setReviewerNotes("Đồng chí giảng dạy rất tốt, vượt chỉ tiêu.");

        // 3. Ban Giám Hiệu kết luận: Hoàn thành nhiệm vụ (điểm 65.0)
        eval.setBghTotalScore(BigDecimal.valueOf(65.0));
        eval.setBghClassification("Hoàn thành nhiệm vụ");

        Nq52EvaluationForm form = new Nq52EvaluationForm();
        form.setId(110L);
        form.setMonth(11);
        form.setYear(2026);
        form.setSelfClassification("Hoàn thành tốt nhiệm vụ");
        form.setReviewerClassification("Hoàn thành xuất sắc nhiệm vụ");
        form.setBghClassification("Hoàn thành nhiệm vụ");
        form.setSelfScores(new HashMap<>());
        form.setReviewerScores(new HashMap<>());
        form.setBghScores(new HashMap<>());
        form.setExplanations(new HashMap<>());

        byte[] docxBytes = service.generateM03Docx(eval, form, List.of());
        assertNotNull(docxBytes);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            var t5 = doc.getTables().get(5);
            var r0 = t5.getRow(0);

            // Dấu X tại Bảng 5 (mục "1. Tự đề xuất xếp loại mức chất lượng") phải CỐ ĐỊNH theo giáo viên tự đánh giá:
            // Cell 0 (Xuất sắc - điểm/xếp loại của Tổ trưởng) KHÔNG ĐƯỢC có dấu X
            assertEquals("", r0.getTableCells().get(0).getParagraphs().get(0).getText().trim(),
                    "Cell 0 (Xuất sắc) không được có X vì giáo viên tự đánh giá mức Tốt");

            // Cell 2 (Tốt - dữ liệu của cá nhân giáo viên tự đánh giá) PHẢI CÓ DẤU X CỐ ĐỊNH
            assertEquals("X", r0.getTableCells().get(2).getParagraphs().get(0).getText().trim(),
                    "Cell 2 (Tốt) phải có dấu X cố định theo dữ liệu người giáo viên tự đánh giá");

            // Cell 4 (Hoàn thành - điểm/xếp loại của BGH) KHÔNG ĐƯỢC có dấu X ở bảng tự đánh giá
            assertEquals("", r0.getTableCells().get(4).getParagraphs().get(0).getText().trim(),
                    "Cell 4 (Hoàn thành) không được có X ở bảng tự đánh giá");

            // Cell 6 (Không hoàn thành) không có X
            assertEquals("", r0.getTableCells().get(6).getParagraphs().get(0).getText().trim());

            // Trong khi đó:
            // - Dòng đề xuất của Tổ trưởng phải hiển thị: Hoàn thành xuất sắc nhiệm vụ
            // - Dòng kết luận của BGH phải hiển thị: Xếp loại: Hoàn thành nhiệm vụ
            boolean foundReviewerProp = false;
            boolean foundBghClass = false;
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText().trim();
                if (t.contains("* Đề xuất mức xếp loại chất lượng:") && t.contains("Hoàn thành xuất sắc nhiệm vụ")) {
                    foundReviewerProp = true;
                }
                if (t.contains("Xếp loại:") && t.contains("Hoàn thành nhiệm vụ")) {
                    foundBghClass = true;
                }
            }
            assertTrue(foundReviewerProp, "Dòng đề xuất của Tổ trưởng phải đúng kết quả Tổ trưởng đánh giá");
            assertTrue(foundBghClass, "Dòng kết luận của BGH phải đúng kết quả BGH đánh giá");
        }
    }
}
