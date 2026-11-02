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

            assertTrue(fullText.contains("ĐÃ KÝ SỐ ĐIỆN TỬ"), "Docx must contain teacher digital signature stamp");
            assertTrue(fullText.contains("Nguyễn Văn An"), "Docx must contain teacher signer name");
            assertTrue(fullText.contains("ĐÃ KÝ SỐ THẨM ĐỊNH TỔ"), "Docx must contain dept leader digital signature stamp");
            assertTrue(fullText.contains("Trần Thị Bình"), "Docx must contain dept leader signer name");
            assertTrue(fullText.contains("ĐÃ KÝ DUYỆT CHÍNH THỨC"), "Docx must contain BGH digital signature stamp");
            assertTrue(fullText.contains("TRƯỜNG THCS NGUYỄN VĂN LINH"), "Docx must contain BGH signer name");
        }
    }
}
