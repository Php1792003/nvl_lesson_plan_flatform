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

            // Kiểm tra con dấu/chữ ký dạng hình ảnh được chèn (3 ảnh)
            assertTrue(wb.getAllPictures().size() >= 3, "Excel export must contain 3 digital signature picture stamps");

            boolean foundBgh = false;
            boolean foundDept = false;
            boolean foundTeacher = false;

            for (int r = 16; r <= sheet.getLastRowNum(); r++) {
                var row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c <= 17; c++) {
                    var cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        String val = cell.getStringCellValue();
                        if (val.contains("Ban Giám Hiệu")) {
                            foundBgh = true;
                            assertTrue(c >= 1 && c <= 4, "BGH name must be in cols 1-4, found at col " + c);
                        }
                        if (val.contains("Lê Hoàng Long")) {
                            foundDept = true;
                            assertTrue(c >= 6 && c <= 10, "Dept leader name must be in cols 6-10, found at col " + c);
                        }
                        if (val.contains("Nguyễn Văn An")) {
                            foundTeacher = true;
                            assertTrue(c >= 13 && c <= 17, "Teacher name must be in cols 13-17, found at col " + c);
                        }
                    }
                }
            }

            assertTrue(foundBgh, "BGH name should be present in Excel export");
            assertTrue(foundDept, "Dept leader name should be present in Excel export");
            assertTrue(foundTeacher, "Teacher name should be present in Excel export");
        }
    }

    @Test
    public void testSigningDateNov04MatchesExactExecutionTime() throws Exception {
        Nq52ExcelExportService service = new Nq52ExcelExportService();

        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setId(11L);
        wr.setEvaluationMonth(11);
        wr.setEvaluationYear(2026);
        wr.setTeacherNameSnapshot("Nguyễn Văn An");

        Nq52WorkResultForm form = new Nq52WorkResultForm();
        form.setId(11L);
        form.setMonth(11);
        form.setYear(2026);

        // Giả lập giáo viên ký số vào ngày 04 tháng 11 năm 2026 lúc 07:00 sáng
        Instant signedAtNov04 = Instant.parse("2026-11-04T00:00:00Z"); // 07:00 AM UTC+7

        Nq52DigitalSignature sigTeacher = new Nq52DigitalSignature();
        sigTeacher.setSigningRole(Nq52SigningRole.TEACHER);
        sigTeacher.setSignerNameSnapshot("Nguyễn Văn An");
        sigTeacher.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sigTeacher.setCaProvider("VNPT_SMARTCA");
        sigTeacher.setSignedAt(signedAtNov04);

        byte[] bytes = service.generateWorkResultExcel(wr, form, List.of(sigTeacher));
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Mau_01TKNV");
            assertNotNull(sheet);

            // Kiểm tra Row 3 có đúng ngày 04 tháng 11 năm 2026
            String row3Date = sheet.getRow(3).getCell(7).getStringCellValue();
            assertTrue(row3Date.contains("ngày 04 tháng 11 năm 2026"),
                    "Row 3 phải chứa ngày ký 'ngày 04 tháng 11 năm 2026', thực tế: " + row3Date);

            // Kiểm tra hàng ngày ký phía trên chữ ký có chứa ngày 04 tháng 11 năm 2026 ở cột 13-17
            boolean foundDateAboveTeacher = false;
            for (int r = 16; r <= sheet.getLastRowNum(); r++) {
                var row = sheet.getRow(r);
                if (row == null) continue;
                var cell = row.getCell(13);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    if (cell.getStringCellValue().contains("ngày 04 tháng 11 năm 2026")) {
                        foundDateAboveTeacher = true;
                        break;
                    }
                }
            }
            assertTrue(foundDateAboveTeacher, "Phía trên chữ ký cá nhân phải có dòng ngày ký 'ngày 04 tháng 11 năm 2026'");
        }
    }

    @Test
    public void testA4LandscapeAndFitToSinglePageWithTimesNewRomanFont() throws Exception {
        Nq52ExcelExportService service = new Nq52ExcelExportService();

        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setId(20L);
        wr.setEvaluationMonth(11);
        wr.setEvaluationYear(2026);
        wr.setTeacherNameSnapshot("Trần Văn B");

        Nq52WorkResultForm form = new Nq52WorkResultForm();
        form.setId(20L);
        form.setMonth(11);
        form.setYear(2026);

        byte[] bytes = service.generateWorkResultExcel(wr, form);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Mau_01TKNV");
            assertNotNull(sheet);

            // 1. Kiểm tra Page Setup: A4, Landscape (Nằm ngang)
            org.apache.poi.ss.usermodel.PrintSetup printSetup = sheet.getPrintSetup();
            assertNotNull(printSetup);
            assertEquals(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE, printSetup.getPaperSize(),
                    "Khổ giấy phải là A4");
            assertTrue(printSetup.getLandscape(), "Layout phải nằm ngang (Landscape)");

            // 2. Kiểm tra Fit to 1 page (Vừa vặn trong 1 trang duy nhất khi in)
            assertTrue(sheet.getFitToPage(), "Phải kích hoạt chế độ FitToPage");
            assertEquals(1, printSetup.getFitWidth(), "Chiều rộng vừa 1 trang");
            assertEquals(1, printSetup.getFitHeight(), "Chiều cao vừa 1 trang");

            // 3. Kiểm tra phông chữ Times New Roman trên các ô tiêu biểu
            var rowTitle = sheet.getRow(5);
            assertNotNull(rowTitle);
            var titleCell = rowTitle.getCell(0);
            assertNotNull(titleCell);
            var titleFont = wb.getFontAt(titleCell.getCellStyle().getFontIndex());
            assertEquals("Times New Roman", titleFont.getFontName(), "Tiêu đề phải dùng phông Times New Roman");

            var rowHeader = sheet.getRow(12);
            assertNotNull(rowHeader);
            var headerCell = rowHeader.getCell(0);
            assertNotNull(headerCell);
            var headerFont = wb.getFontAt(headerCell.getCellStyle().getFontIndex());
            assertEquals("Times New Roman", headerFont.getFontName(), "Tiêu đề bảng phải dùng phông Times New Roman");
        }
    }

    @Test
    public void testGenerateEvaluationSummaryExcel() throws Exception {
        Nq52ExcelExportService service = new Nq52ExcelExportService();

        com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryDto summary = new com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryDto();
        summary.setMonth(11);
        summary.setYear(2026);
        summary.setDepartmentName("Tổ Toán - Tin");
        summary.setCampusName("Điểm trường chính");

        List<com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryItemDto> items = new ArrayList<>();

        var i1 = new com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryItemDto();
        i1.setStt(1);
        i1.setFullName("Trần Thị Thu Hằng");
        i1.setBirthYear("1971");
        i1.setPositionTitle("Phó Hiệu trưởng");
        i1.setBghClassification("Hoàn thành tốt nhiệm vụ");
        items.add(i1);

        var i2 = new com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryItemDto();
        i2.setStt(2);
        i2.setFullName("Nguyễn Văn An");
        i2.setBirthYear("1985");
        i2.setPositionTitle("Giáo viên Toán – Tin");
        i2.setBghClassification("Hoàn thành xuất sắc nhiệm vụ");
        items.add(i2);

        var i3 = new com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryItemDto();
        i3.setStt(3);
        i3.setFullName("Lê Hoàng Long");
        i3.setBirthYear("1988");
        i3.setPositionTitle("Tổ trưởng Toán - Tin");
        i3.setBghClassification("Hoàn thành nhiệm vụ");
        items.add(i3);

        var i4 = new com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryItemDto();
        i4.setStt(4);
        i4.setFullName("Phạm Thị Minh Chi");
        i4.setBirthYear("1981");
        i4.setPositionTitle("Giáo viên Toán – Tin");
        i4.setBghClassification("Không hoàn thành nhiệm vụ");
        items.add(i4);

        summary.setItems(items);
        summary.setTotalCount(4);
        summary.setExcellentCount(1);
        summary.setGoodCount(1);
        summary.setSatisfactoryCount(1);
        summary.setUnsatisfactoryCount(1);
        summary.setBghSignedCount(1);
        summary.setBghSignerName("Võ Công Hải");
        summary.setBghSignedAtFormatted("09/11/2026 10:30");

        byte[] bytes = service.generateEvaluationSummaryExcel(summary);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Tong_hop_xep_loai_NQ52");
            assertNotNull(sheet);

            // 1. Kiểm tra Page Setup: A4, Landscape (Nằm ngang)
            var printSetup = sheet.getPrintSetup();
            assertEquals(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE, printSetup.getPaperSize());
            assertTrue(printSetup.getLandscape());
            assertTrue(sheet.getFitToPage());
            assertEquals(1, printSetup.getFitWidth());
            assertEquals(1, printSetup.getFitHeight());

            // 2. Kiểm tra Tiêu đề
            var rowTitle = sheet.getRow(0);
            assertNotNull(rowTitle);
            assertTrue(rowTitle.getCell(0).getStringCellValue().contains("THÁNG 11/2026"));

            // 3. Kiểm tra phông Times New Roman
            var titleFont = wb.getFontAt(rowTitle.getCell(0).getCellStyle().getFontIndex());
            assertEquals("Times New Roman", titleFont.getFontName());

            // 4. Kiểm tra Header 2 tầng (Row 3, 4)
            var rowHeader1 = sheet.getRow(3);
            var rowHeader2 = sheet.getRow(4);
            assertEquals("STT", rowHeader1.getCell(0).getStringCellValue());
            assertEquals("Họ và tên", rowHeader1.getCell(1).getStringCellValue());
            assertEquals("Năm sinh", rowHeader1.getCell(2).getStringCellValue());
            assertEquals("Mức độ xếp loại chất lượng", rowHeader1.getCell(4).getStringCellValue());
            assertTrue(rowHeader2.getCell(4).getStringCellValue().contains("Xuất sắc"));
            assertTrue(rowHeader2.getCell(5).getStringCellValue().contains("tốt"));

            // 5. Kiểm tra dòng 1: Trần Thị Thu Hằng (Hoàn thành tốt -> Col 5 có 'x')
            var row1 = sheet.getRow(5);
            assertEquals("Trần Thị Thu Hằng", row1.getCell(1).getStringCellValue());
            assertEquals("1971", row1.getCell(2).getStringCellValue());
            assertEquals("Phó Hiệu trưởng", row1.getCell(3).getStringCellValue());
            assertEquals("", row1.getCell(4).getStringCellValue());
            assertEquals("x", row1.getCell(5).getStringCellValue());

            // 6. Kiểm tra dòng 2: Nguyễn Văn An (Xuất sắc -> Col 4 có 'x')
            var row2 = sheet.getRow(6);
            assertEquals("Nguyễn Văn An", row2.getCell(1).getStringCellValue());
            assertEquals("x", row2.getCell(4).getStringCellValue());
            assertEquals("", row2.getCell(5).getStringCellValue());

            // 7. Kiểm tra dòng Tổng cộng (Row 9)
            var rowTotal = sheet.getRow(9);
            assertEquals("TỔNG CỘNG", rowTotal.getCell(0).getStringCellValue());
            assertEquals("1", rowTotal.getCell(4).getStringCellValue());
            assertEquals("1", rowTotal.getCell(5).getStringCellValue());
            assertEquals("1", rowTotal.getCell(6).getStringCellValue());
            assertEquals("1", rowTotal.getCell(7).getStringCellValue());
        }
    }
}
