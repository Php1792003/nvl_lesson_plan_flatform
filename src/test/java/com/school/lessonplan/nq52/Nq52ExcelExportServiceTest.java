package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
import com.school.lessonplan.nq52.dto.Nq52WorkResultTaskItem;
import com.school.lessonplan.nq52.signature.Nq52DigitalSignature;
import com.school.lessonplan.nq52.signature.Nq52SignatureType;
import com.school.lessonplan.nq52.signature.Nq52SigningRole;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Nq52ExcelExportServiceTest {

    @Test
    public void testGenerateWorkResultExcel() throws Exception {
        Nq52ExcelExportService service = new Nq52ExcelExportService();

        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setId(1L);
        wr.setEvaluationMonth(11);
        wr.setEvaluationYear(2026);
        wr.setTeacherNameSnapshot("Nguyễn Văn A");
        wr.setPositionTitle("Giáo viên Toán");
        wr.setDepartmentNameSnapshot("Toán học");
        wr.setCampusNameSnapshot("Điểm trường chính");
        wr.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        wr.setEvaluationDateStr("Cẩm Lệ, ngày 30 tháng 11 năm 2026");
        wr.setReviewerNameSnapshot("Lê Hoàng Long");

        Nq52WorkResultForm form = new Nq52WorkResultForm();
        form.setId(1L);
        form.setMonth(11);
        form.setYear(2026);
        form.setPositionTitle("Giáo viên Toán");
        form.setWorkUnit("Trường THCS Nguyễn Văn Linh");
        form.setEvaluationDateStr("Cẩm Lệ, ngày 30 tháng 11 năm 2026");
        form.setSummaryNotes("Hoàn thành xuất sắc toàn bộ nhiệm vụ.");

        List<Nq52WorkResultTaskItem> tasks = new ArrayList<>();
        Nq52WorkResultTaskItem t1 = new Nq52WorkResultTaskItem("1", "I", 1, "Thực hiện giảng dạy đúng chương trình môn Toán");
        t1.setNature("NORMAL");
        t1.setRole("LEAD");
        t1.setStatus("COMPLETED");
        t1.setTimeRequired("01/11 - 30/11");
        t1.setTimeProgress("ON_TIME");
        t1.setQuality("STANDARD");
        tasks.add(t1);

        Nq52WorkResultTaskItem t2 = new Nq52WorkResultTaskItem("2", "II", 1, "Bồi dưỡng học sinh giỏi đạt giải cấp quận");
        t2.setNature("COMPLEX");
        t2.setRole("LEAD");
        t2.setStatus("COMPLETED");
        t2.setTimeRequired("Thường xuyên");
        t2.setTimeProgress("EARLY");
        t2.setQuality("EXCEEDED");
        t2.setSuggestions("Đề xuất khen thưởng");
        t2.setExplanation("Đạt 3 giải Nhất");
        tasks.add(t2);

        form.setTasks(tasks);
        form.setTotalAssigned(2);
        form.setTotalCompleted(2);
        form.setTotalUncompleted(0);
        form.setCompletionRate(100.0);
        form.setProgressEarly(1);
        form.setProgressOnTime(1);
        form.setProgressLate(0);
        form.setOnTimeRate(100.0);
        form.setQualityStandard(1);
        form.setQualityExceeded(1);
        form.setQualitySubstandard(0);
        form.setQualityRate(100.0);

        byte[] bytes = service.generateWorkResultExcel(wr, form);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Verify content by reading back
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Mau_01TKNV");
            assertNotNull(sheet);

            // Row 0: Mẫu 01TKNV
            assertEquals("Mẫu 01TKNV", sheet.getRow(0).getCell(15).getStringCellValue());

            // Check metadata (Row 9: Teacher Name)
            assertTrue(sheet.getRow(9).getCell(0).getStringCellValue().contains("Nguyễn Văn A"));

            // Check headers (Row 12: TT)
            assertEquals("TT", sheet.getRow(12).getCell(0).getStringCellValue());
            assertEquals("Nội dung công việc/sản phẩm được giao trong kỳ đánh giá", sheet.getRow(12).getCell(1).getStringCellValue());

            // Save to disk for inspection
            try (FileOutputStream fos = new FileOutputStream("build/test_export.xlsx")) {
                fos.write(bytes);
            }
        }
    }

    @Test
    public void testGenerateWorkResultExcelWithDigitalSignatures() throws Exception {
        Nq52ExcelExportService service = new Nq52ExcelExportService();

        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setId(10L);
        wr.setEvaluationMonth(11);
        wr.setEvaluationYear(2026);
        wr.setTeacherNameSnapshot("Nguyễn Văn An");

        Nq52WorkResultForm form = new Nq52WorkResultForm();
        form.setId(10L);
        form.setMonth(11);
        form.setYear(2026);
        form.setTasks(new ArrayList<>());

        List<Nq52DigitalSignature> sigs = new ArrayList<>();

        Nq52DigitalSignature sigTeacher = new Nq52DigitalSignature();
        sigTeacher.setSigningRole(Nq52SigningRole.TEACHER);
        sigTeacher.setSignerNameSnapshot("Nguyễn Văn An");
        sigTeacher.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigTeacher.setCaProvider("VNPT_SMARTCA");
        sigTeacher.setSignedAt(Instant.parse("2026-11-01T10:15:00Z"));
        sigs.add(sigTeacher);

        Nq52DigitalSignature sigDept = new Nq52DigitalSignature();
        sigDept.setSigningRole(Nq52SigningRole.DEPT_LEADER);
        sigDept.setSignerNameSnapshot("Lê Hoàng Long");
        sigDept.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigDept.setCaProvider("VIETTEL_CLOUD_CA");
        sigDept.setSignedAt(Instant.parse("2026-11-02T14:20:00Z"));
        sigs.add(sigDept);

        Nq52DigitalSignature sigBgh = new Nq52DigitalSignature();
        sigBgh.setSigningRole(Nq52SigningRole.PRINCIPAL);
        sigBgh.setSignerNameSnapshot("TRƯỜNG THCS NGUYỄN VĂN LINH");
        sigBgh.setSignatureType(Nq52SignatureType.USB_TOKEN);
        sigBgh.setCaProvider("BAN_CO_YEU_CHINH_PHU");
        sigBgh.setSignedAt(Instant.parse("2026-11-02T16:00:00Z"));
        sigs.add(sigBgh);

        byte[] bytes = service.generateWorkResultExcel(wr, form, sigs);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Mau_01TKNV");
            assertNotNull(sheet);

            // Find rows where stamps are placed (around summary row + 5)
            // Look for signature stamps text in sheet
            boolean foundBgh = false;
            boolean foundDept = false;
            boolean foundTeacher = false;

            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                var row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c <= 17; c++) {
                    var cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        String val = cell.getStringCellValue();
                        if (val.contains("🏛️ ĐÃ KÝ SỐ DUYỆT BGH")) {
                            foundBgh = true;
                            assertTrue(c >= 1 && c <= 4, "BGH stamp header must be in cols 1-4, found at col " + c);
                        }
                        if (val.contains("✍️ ĐÃ KÝ SỐ THẨM ĐỊNH TỔ")) {
                            foundDept = true;
                            assertTrue(c >= 6 && c <= 10, "Dept leader stamp header must be in cols 6-10, found at col " + c);
                        }
                        if (val.contains("✍️ ĐÃ KÝ SỐ ĐIỆN TỬ")) {
                            foundTeacher = true;
                            assertTrue(c >= 13 && c <= 17, "Teacher stamp header must be in cols 13-17, found at col " + c);
                        }
                    }
                }
            }

            assertTrue(foundBgh, "BGH stamp should be present in Excel export");
            assertTrue(foundDept, "Dept leader stamp should be present in Excel export");
            assertTrue(foundTeacher, "Teacher stamp should be present in Excel export");
        }
    }
}
