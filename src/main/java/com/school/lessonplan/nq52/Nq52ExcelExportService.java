package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryDto;
import com.school.lessonplan.nq52.dto.Nq52EvaluationSummaryItemDto;
import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
import com.school.lessonplan.nq52.dto.Nq52WorkResultTaskItem;
import com.school.lessonplan.nq52.signature.Nq52DigitalSignature;
import com.school.lessonplan.nq52.signature.Nq52SignatureStatus;
import com.school.lessonplan.nq52.signature.Nq52SigningRole;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Nq52ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(Nq52ExcelExportService.class);
    private static final DateTimeFormatter SIG_DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public byte[] generateWorkResultExcel(Nq52WorkResult result, Nq52WorkResultForm form) {
        return generateWorkResultExcel(result, form, Collections.emptyList());
    }

    public byte[] generateWorkResultExcel(Nq52WorkResult result, Nq52WorkResultForm form, List<Nq52DigitalSignature> signatures) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Mau_01TKNV");
            sheet.setDisplayGridlines(true);

            // Cấu hình 18 cột (A - R) tối ưu tỷ lệ hiển thị trên khổ giấy A4 nằm ngang
            sheet.setColumnWidth(0, (int) (4.5 * 256));   // A: TT
            sheet.setColumnWidth(1, (int) (35.0 * 256));  // B: Nội dung công việc
            sheet.setColumnWidth(2, (int) (6.5 * 256));   // C: Bình thường / (1) Được giao
            sheet.setColumnWidth(3, (int) (6.5 * 256));   // D: Phức tạp / (2) Hoàn thành
            sheet.setColumnWidth(4, (int) (7.0 * 256));   // E: Rất phức tạp / (3) Chưa hoàn thành
            sheet.setColumnWidth(5, (int) (7.5 * 256));   // F: Chủ trì / (4) Tỷ lệ HT
            sheet.setColumnWidth(6, (int) (7.0 * 256));   // G: Phối hợp / (5) Sớm hạn
            sheet.setColumnWidth(7, (int) (7.0 * 256));   // H: Đã HT / (6) Đúng hạn
            sheet.setColumnWidth(8, (int) (7.0 * 256));   // I: Chưa HT / (7) Trễ hạn
            sheet.setColumnWidth(9, (int) (9.0 * 256));   // J: TG yêu cầu / (8) Tỷ lệ đúng hạn part 1
            sheet.setColumnWidth(10, (int) (6.5 * 256));  // K: Sớm hạn / (8) Tỷ lệ đúng hạn part 2
            sheet.setColumnWidth(11, (int) (6.5 * 256));  // L: Đúng hạn / (9) Đảm bảo
            sheet.setColumnWidth(12, (int) (6.5 * 256));  // M: Trễ hạn / (10) Vượt mức part 1
            sheet.setColumnWidth(13, (int) (7.5 * 256));  // N: Chưa đảm bảo / (10) Vượt mức part 2
            sheet.setColumnWidth(14, (int) (7.5 * 256));  // O: Đảm bảo / (11) Chưa đảm bảo
            sheet.setColumnWidth(15, (int) (7.5 * 256));  // P: Vượt mức / (12) Tỷ lệ chất lượng part 1
            sheet.setColumnWidth(16, (int) (9.5 * 256));  // Q: Kiến nghị / (12) Tỷ lệ chất lượng part 2
            sheet.setColumnWidth(17, (int) (12.0 * 256)); // R: Giải trình / (13) Ghi chú

            // =========================================================================
            // CẤU HÌNH TRANG IN (PAGE SETUP): KHỔ A4 NẰM NGANG, FIT GỌN VÀO 1 TRANG DUY NHẤT
            // =========================================================================
            PrintSetup printSetup = sheet.getPrintSetup();
            printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
            printSetup.setLandscape(true);
            sheet.setAutobreaks(true);
            sheet.setFitToPage(true);
            printSetup.setFitWidth((short) 1);
            printSetup.setFitHeight((short) 1);

            // Căn lề chuẩn trang in (margins) để vừa vặn tuyệt đối trên khổ A4 ngang
            sheet.setMargin(Sheet.LeftMargin, 0.35);   // ~8.9 mm
            sheet.setMargin(Sheet.RightMargin, 0.35);  // ~8.9 mm
            sheet.setMargin(Sheet.TopMargin, 0.35);    // ~8.9 mm
            sheet.setMargin(Sheet.BottomMargin, 0.35); // ~8.9 mm
            sheet.setMargin(Sheet.HeaderMargin, 0.15);
            sheet.setMargin(Sheet.FooterMargin, 0.15);

            // Căn giữa trang theo chiều ngang khi in
            sheet.setHorizontallyCenter(true);

            // =========================================================================
            // PHÔNG CHỮ: 100% PHÔNG TIMES NEW ROMAN CHO TOÀN BỘ WORKBOOK VÀ CELL STYLES
            // =========================================================================
            Font defaultFont = workbook.getFontAt(0);
            if (defaultFont != null) {
                defaultFont.setFontName("Times New Roman");
            }

            Font fontRegular = workbook.createFont();
            fontRegular.setFontName("Times New Roman");
            fontRegular.setFontHeightInPoints((short) 10);

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Times New Roman");
            fontBold.setFontHeightInPoints((short) 10);
            fontBold.setBold(true);

            Font fontItalic = workbook.createFont();
            fontItalic.setFontName("Times New Roman");
            fontItalic.setFontHeightInPoints((short) 10);
            fontItalic.setItalic(true);

            Font fontSmallItalic = workbook.createFont();
            fontSmallItalic.setFontName("Times New Roman");
            fontSmallItalic.setFontHeightInPoints((short) 9);
            fontSmallItalic.setItalic(true);

            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Times New Roman");
            fontTitle.setFontHeightInPoints((short) 12);
            fontTitle.setBold(true);

            Font fontHeader = workbook.createFont();
            fontHeader.setFontName("Times New Roman");
            fontHeader.setFontHeightInPoints((short) 9);
            fontHeader.setBold(true);

            // Cell Styles
            CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(fontHeader);
            tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tableHeaderStyle.setWrapText(true);
            setBorders(tableHeaderStyle);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            sectionHeaderStyle.setFont(fontBold);
            sectionHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
            sectionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(sectionHeaderStyle);
            sectionHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle cellStyleLeft = workbook.createCellStyle();
            cellStyleLeft.setFont(fontRegular);
            cellStyleLeft.setAlignment(HorizontalAlignment.LEFT);
            cellStyleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyleLeft.setWrapText(true);
            setBorders(cellStyleLeft);

            CellStyle cellStyleCenter = workbook.createCellStyle();
            cellStyleCenter.setFont(fontRegular);
            cellStyleCenter.setAlignment(HorizontalAlignment.CENTER);
            cellStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyleCenter.setWrapText(true);
            setBorders(cellStyleCenter);

            CellStyle cellStyleCenterBold = workbook.createCellStyle();
            cellStyleCenterBold.setFont(fontBold);
            cellStyleCenterBold.setAlignment(HorizontalAlignment.CENTER);
            cellStyleCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyleCenterBold.setWrapText(true);
            setBorders(cellStyleCenterBold);

            CellStyle metaLabelBold = workbook.createCellStyle();
            metaLabelBold.setFont(fontBold);
            metaLabelBold.setAlignment(HorizontalAlignment.LEFT);
            metaLabelBold.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaCenter = workbook.createCellStyle();
            metaCenter.setFont(fontRegular);
            metaCenter.setAlignment(HorizontalAlignment.CENTER);
            metaCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaCenterBold = workbook.createCellStyle();
            metaCenterBold.setFont(fontBold);
            metaCenterBold.setAlignment(HorizontalAlignment.CENTER);
            metaCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaCenterItalic = workbook.createCellStyle();
            metaCenterItalic.setFont(fontItalic);
            metaCenterItalic.setAlignment(HorizontalAlignment.CENTER);
            metaCenterItalic.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaCenterSmallItalic = workbook.createCellStyle();
            metaCenterSmallItalic.setFont(fontSmallItalic);
            metaCenterSmallItalic.setAlignment(HorizontalAlignment.CENTER);
            metaCenterSmallItalic.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(fontTitle);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            int rowIdx = 0;

            // Row 0: Mẫu 01TKNV
            Row r0 = sheet.createRow(rowIdx++);
            Cell c0 = r0.createCell(15);
            c0.setCellValue("Mẫu 01TKNV");
            CellStyle s0 = workbook.createCellStyle();
            s0.setFont(fontBold);
            s0.setAlignment(HorizontalAlignment.RIGHT);
            c0.setCellStyle(s0);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 15, 17));

            // Row 1: Header agency & slogan
            Row r1 = sheet.createRow(rowIdx++);
            Cell c1_1 = r1.createCell(0);
            c1_1.setCellValue("UBND PHƯỜNG CẨM LỆ");
            c1_1.setCellStyle(metaCenter);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            Cell c1_2 = r1.createCell(7);
            c1_2.setCellValue("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
            c1_2.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 7, 17));

            // Row 2: School & motto
            Row r2 = sheet.createRow(rowIdx++);
            Cell c2_1 = r2.createCell(0);
            c2_1.setCellValue("TRƯỜNG THCS NGUYỄN VĂN LINH");
            c2_1.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 6));

            Cell c2_2 = r2.createCell(7);
            c2_2.setCellValue("Độc lập - Tự do - Hạnh phúc");
            c2_2.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 7, 17));

            // Xác định chữ ký số của từng bên sớm để lấy ngày ký chính xác
            Nq52DigitalSignature sigTeacher = null;
            Nq52DigitalSignature sigDeptLeader = null;
            Nq52DigitalSignature sigPrincipal = null;

            if (signatures != null) {
                for (Nq52DigitalSignature s : signatures) {
                    if (s.getStatus() == Nq52SignatureStatus.VALID) {
                        if (s.getSigningRole() == Nq52SigningRole.TEACHER) sigTeacher = s;
                        else if (s.getSigningRole() == Nq52SigningRole.DEPT_LEADER) sigDeptLeader = s;
                        else if (s.getSigningRole() == Nq52SigningRole.PRINCIPAL) sigPrincipal = s;
                    }
                }
            }

            ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDate todayVn = LocalDate.now(vnZone);

            LocalDate teacherDate = todayVn;
            if (sigTeacher != null && sigTeacher.getSignedAt() != null) {
                teacherDate = sigTeacher.getSignedAt().atZone(vnZone).toLocalDate();
            } else if (result.getSubmittedAt() != null) {
                teacherDate = result.getSubmittedAt().atZone(vnZone).toLocalDate();
            }
            String teacherDateLine = "Cẩm Lệ, ngày " + String.format("%02d", teacherDate.getDayOfMonth())
                    + " tháng " + String.format("%02d", teacherDate.getMonthValue())
                    + " năm " + teacherDate.getYear();

            // Row 3: Date
            Row r3 = sheet.createRow(rowIdx++);
            Cell c3 = r3.createCell(7);
            c3.setCellValue(teacherDateLine);
            c3.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 7, 17));

            Row rBlank1 = sheet.createRow(rowIdx++);
            rBlank1.setHeightInPoints(6f);

            // Row 5: Title
            Row rTitle = sheet.createRow(rowIdx++);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue("BẢNG THỐNG KÊ, TỰ ĐÁNH GIÁ KẾT QUẢ THỰC HIỆN NHIỆM VỤ CỦA CÁ NHÂN VIÊN CHỨC");
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 17));

            // Row 6: Month/Year
            Row rSub = sheet.createRow(rowIdx++);
            Cell cSub = rSub.createCell(0);
            cSub.setCellValue("THÁNG " + result.getEvaluationMonth() + "/NĂM " + result.getEvaluationYear());
            cSub.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 17));

            // Row 7: Note
            Row rNote = sheet.createRow(rowIdx++);
            Cell cNote = rNote.createCell(0);
            cNote.setCellValue("(Kèm theo Phiếu đánh giá, xếp loại chất lượng hàng tháng của cá nhân)");
            cNote.setCellStyle(metaCenterSmallItalic);
            sheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 17));

            Row rBlank2 = sheet.createRow(rowIdx++);
            rBlank2.setHeightInPoints(6f);

            // Teacher Metadata
            Row rMeta1 = sheet.createRow(rowIdx++);
            Cell cm1 = rMeta1.createCell(0);
            cm1.setCellValue("Họ và tên viên chức: " + (result.getTeacherNameSnapshot() != null ? result.getTeacherNameSnapshot() : "")
                    + "    |    Chức danh, chức vụ: " + (result.getPositionTitle() != null ? result.getPositionTitle() : "Giáo viên"));
            cm1.setCellStyle(metaLabelBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 17));

            Row rMeta2 = sheet.createRow(rowIdx++);
            String campusText = result.getCampusNameSnapshot() != null ? result.getCampusNameSnapshot() : "Điểm trường chính";
            Cell cm2 = rMeta2.createCell(0);
            cm2.setCellValue("Tổ chuyên môn: " + (result.getDepartmentNameSnapshot() != null ? result.getDepartmentNameSnapshot() : "Toán học")
                    + "    |    Điểm trường: " + campusText
                    + "    |    Đơn vị: " + (result.getWorkUnit() != null ? result.getWorkUnit() : "Trường THCS Nguyễn Văn Linh"));
            cm2.setCellStyle(metaLabelBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 17));

            Row rBlank3 = sheet.createRow(rowIdx++);
            rBlank3.setHeightInPoints(6f);

            // ==========================================
            // Main Table Header Rows (Multi-level 3 rows)
            // ==========================================
            int headerStart = rowIdx;
            Row h1 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row h2 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row h3 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            h1.setHeightInPoints(24f);
            h2.setHeightInPoints(24f);
            h3.setHeightInPoints(18f);

            // Col 0: TT (Span 3 rows)
            h1.getCell(0).setCellValue("TT");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart + 2, 0, 0));

            // Col 1: Nội dung (Span 3 rows)
            h1.getCell(1).setCellValue("Nội dung công việc/sản phẩm được giao trong kỳ đánh giá");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart + 2, 1, 1));

            // Col 2-4: Tính chất công việc
            h1.getCell(2).setCellValue("Tính chất công việc\n(Đánh dấu X vào ô tương ứng)");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart, 2, 4));
            h2.getCell(2).setCellValue("Bình thường");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 2, 2));
            h2.getCell(3).setCellValue("Phức tạp");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 3, 3));
            h2.getCell(4).setCellValue("Rất phức tạp");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 4, 4));

            // Col 5-6: Mức độ tham gia thực hiện
            h1.getCell(5).setCellValue("Mức độ tham gia thực hiện\n(Đánh dấu X vào ô tương ứng)");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart, 5, 6));
            h2.getCell(5).setCellValue("Chủ trì thực hiện");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 5, 5));
            h2.getCell(6).setCellValue("Phối hợp thực hiện");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 6, 6));

            // Col 7-8: Sản phẩm, công việc đã thực hiện
            h1.getCell(7).setCellValue("Sản phẩm, công việc đã thực hiện trong kỳ đánh giá");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart, 7, 8));
            h2.getCell(7).setCellValue("Đã hoàn thành");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 7, 7));
            h2.getCell(8).setCellValue("Chưa hoàn thành");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 8, 8));

            // Col 9-12: Thời gian thực hiện
            h1.getCell(9).setCellValue("Thời gian thực hiện");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart, 9, 12));

            h2.getCell(9).setCellValue("Thời gian yêu cầu của nhiệm vụ đặt ra");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 9, 9));

            h2.getCell(10).setCellValue("Thời gian hoàn thành");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 1, 10, 12));
            h3.getCell(10).setCellValue("Sớm hạn");
            h3.getCell(11).setCellValue("Đúng hạn");
            h3.getCell(12).setCellValue("Trễ hạn");

            // Col 13-15: Chất lượng sản phẩm, công việc
            h1.getCell(13).setCellValue("Chất lượng sản phẩm, công việc đã hoàn thành");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart, 13, 15));

            h2.getCell(13).setCellValue("Hoàn thành nhưng chưa đảm bảo chất lượng theo yêu cầu");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 13, 13));

            h2.getCell(14).setCellValue("Hoàn thành đảm bảo chất lượng theo yêu cầu");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 14, 14));

            h2.getCell(15).setCellValue("Hoàn thành đảm bảo chất lượng vượt mức theo yêu cầu");
            sheet.addMergedRegion(new CellRangeAddress(headerStart + 1, headerStart + 2, 15, 15));

            // Col 16: Kiến nghị
            h1.getCell(16).setCellValue("Kiến nghị, đề xuất\n(nếu có)");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart + 2, 16, 16));

            // Col 17: Giải trình
            h1.getCell(17).setCellValue("Giải trình\n(nếu có)");
            sheet.addMergedRegion(new CellRangeAddress(headerStart, headerStart + 2, 17, 17));

            // ==========================================
            // Write Task Rows grouped by Category I, II, III, IV
            // ==========================================
            List<Nq52WorkResultTaskItem> taskList = form.getTasks() != null ? form.getTasks() : new ArrayList<>();
            Map<String, List<Nq52WorkResultTaskItem>> groupedTasks = taskList.stream()
                    .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory() : "I"));

            String[] categories = {"I", "II", "III", "IV"};
            String[] categoryTitles = {
                    "I. Các nhiệm vụ theo chương trình, kế hoạch đã đề ra (từ kế hoạch công tác tháng/năm của đơn vị)",
                    "II. Các nhiệm vụ chuyên môn, thường xuyên theo vị trí việc làm",
                    "III. Các nhiệm vụ đột xuất, phát sinh được giao theo sự phân công của lãnh đạo đơn vị",
                    "IV. Các nhiệm vụ khác: Hội họp, tham gia tập huấn, đào tạo, bồi dưỡng, tham gia hoạt động khác theo phân công của cấp có thẩm quyền…"
            };

            for (int i = 0; i < categories.length; i++) {
                String cat = categories[i];
                Row rCat = createStyledRow(sheet, rowIdx++, 18, sectionHeaderStyle);
                rCat.getCell(0).setCellValue(cat);
                rCat.getCell(1).setCellValue(categoryTitles[i]);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 17));

                List<Nq52WorkResultTaskItem> rawCatTasks = groupedTasks.get(cat);
                List<Nq52WorkResultTaskItem> catTasks = (rawCatTasks != null)
                        ? rawCatTasks.stream().filter(t -> t.getContent() != null && !t.getContent().trim().isEmpty()).toList()
                        : Collections.emptyList();

                if (!catTasks.isEmpty()) {
                    int subIdx = 1;
                    for (Nq52WorkResultTaskItem task : catTasks) {
                        Row rTask = createStyledRow(sheet, rowIdx++, 18, cellStyleCenter);
                        rTask.getCell(1).setCellStyle(cellStyleLeft);
                        rTask.getCell(9).setCellStyle(cellStyleLeft);
                        rTask.getCell(16).setCellStyle(cellStyleLeft);
                        rTask.getCell(17).setCellStyle(cellStyleLeft);

                        // TT & Nội dung
                        rTask.getCell(0).setCellValue(subIdx++);
                        rTask.getCell(1).setCellValue(task.getContent() != null ? task.getContent() : "");

                        // Tính chất
                        if ("NORMAL".equalsIgnoreCase(task.getNature())) rTask.getCell(2).setCellValue("X");
                        else if ("COMPLEX".equalsIgnoreCase(task.getNature())) rTask.getCell(3).setCellValue("X");
                        else if ("VERY_COMPLEX".equalsIgnoreCase(task.getNature())) rTask.getCell(4).setCellValue("X");

                        // Mức độ tham gia
                        if ("LEAD".equalsIgnoreCase(task.getRole())) rTask.getCell(5).setCellValue("X");
                        else if ("COORDINATE".equalsIgnoreCase(task.getRole())) rTask.getCell(6).setCellValue("X");

                        // Sản phẩm
                        if ("COMPLETED".equalsIgnoreCase(task.getStatus())) rTask.getCell(7).setCellValue("X");
                        else if ("UNCOMPLETED".equalsIgnoreCase(task.getStatus())) rTask.getCell(8).setCellValue("X");

                        // Thời gian thực hiện
                        rTask.getCell(9).setCellValue(task.getTimeRequired() != null ? task.getTimeRequired() : "");
                        if ("EARLY".equalsIgnoreCase(task.getTimeProgress())) rTask.getCell(10).setCellValue("X");
                        else if ("ON_TIME".equalsIgnoreCase(task.getTimeProgress())) rTask.getCell(11).setCellValue("X");
                        else if ("LATE".equalsIgnoreCase(task.getTimeProgress())) rTask.getCell(12).setCellValue("X");

                        // Chất lượng sản phẩm
                        if ("SUBSTANDARD".equalsIgnoreCase(task.getQuality())) rTask.getCell(13).setCellValue("X");
                        else if ("STANDARD".equalsIgnoreCase(task.getQuality())) rTask.getCell(14).setCellValue("X");
                        else if ("EXCEEDED".equalsIgnoreCase(task.getQuality())) rTask.getCell(15).setCellValue("X");

                        // Kiến nghị & Giải trình
                        rTask.getCell(16).setCellValue(task.getSuggestions() != null ? task.getSuggestions() : "");
                        rTask.getCell(17).setCellValue(task.getExplanation() != null ? task.getExplanation() : "");
                    }
                } else {
                    // Để 1 dòng trống cho phần này
                    Row rEmpty = createStyledRow(sheet, rowIdx++, 18, cellStyleCenter);
                    rEmpty.setHeightInPoints(18f);
                    rEmpty.getCell(0).setCellValue(1);
                    rEmpty.getCell(1).setCellValue("");
                }
            }

            Row rBlank4 = sheet.createRow(rowIdx++);
            rBlank4.setHeightInPoints(6f);

            // ==========================================
            // Summary Statistics Section (Khớp 100% Mẫu chuẩn)
            // ==========================================
            Row rSumTitle = sheet.createRow(rowIdx++);
            Cell cSumTitle = rSumTitle.createCell(0);
            cSumTitle.setCellValue("TỔNG SỐ SẢN PHẨM/CÔNG VIỆC:");
            cSumTitle.setCellStyle(metaLabelBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 17));

            int sumStart = rowIdx;
            Row sh1 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row sh2 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row sh3 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row sVal = createStyledRow(sheet, rowIdx++, 18, cellStyleCenterBold);
            sh1.setHeightInPoints(20f);
            sh2.setHeightInPoints(20f);
            sh3.setHeightInPoints(16f);
            sVal.setHeightInPoints(18f);
            sVal.getCell(17).setCellStyle(cellStyleLeft); // Ghi chú căn lề trái

            // Col 0-1 (A:B): Để trống khớp với phần nội dung
            sheet.addMergedRegion(new CellRangeAddress(sumStart, sumStart + 2, 0, 1));
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 3, sumStart + 3, 0, 1));

            // C28:F28 -> Tổng số lượng sản phẩm, công việc (Ghi tổng số lượng tương ứng)
            sh1.getCell(2).setCellValue("Tổng số lượng sản phẩm, công việc\n(Ghi tổng số lượng tương ứng)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart, sumStart, 2, 5));

            sh2.getCell(2).setCellValue("Được giao");
            sh3.getCell(2).setCellValue("(1)");
            sVal.getCell(2).setCellValue(form.getTotalAssigned());

            sh2.getCell(3).setCellValue("Hoàn thành");
            sh3.getCell(3).setCellValue("(2)");
            sVal.getCell(3).setCellValue(form.getTotalCompleted());

            sh2.getCell(4).setCellValue("Chưa hoàn thành");
            sh3.getCell(4).setCellValue("(3)");
            sVal.getCell(4).setCellValue(form.getTotalUncompleted());

            sh2.getCell(5).setCellValue("Tỷ lệ hoàn thành (%)\n(4) = (2)/(1) x 100");
            sh3.getCell(5).setCellValue("(4)");
            sVal.getCell(5).setCellValue(String.format("%.1f%%", form.getCompletionRate()));

            // G28:K28 -> Tiến độ sản phẩm, công việc (Ghi tổng số lượng tương ứng)
            sh1.getCell(6).setCellValue("Tiến độ sản phẩm, công việc\n(Ghi tổng số lượng tương ứng)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart, sumStart, 6, 10));

            sh2.getCell(6).setCellValue("Sớm hạn");
            sh3.getCell(6).setCellValue("(5)");
            sVal.getCell(6).setCellValue(form.getProgressEarly());

            sh2.getCell(7).setCellValue("Đúng hạn");
            sh3.getCell(7).setCellValue("(6)");
            sVal.getCell(7).setCellValue(form.getProgressOnTime());

            sh2.getCell(8).setCellValue("Trễ hạn");
            sh3.getCell(8).setCellValue("(7)");
            sVal.getCell(8).setCellValue(form.getProgressLate());

            sh2.getCell(9).setCellValue("Tỷ lệ đúng/sớm hạn (%)\n(8) = (5+6)/(1) x 100");
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 1, sumStart + 1, 9, 10));
            sh3.getCell(9).setCellValue("(8)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 2, sumStart + 2, 9, 10));
            sVal.getCell(9).setCellValue(String.format("%.1f%%", form.getOnTimeRate()));
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 3, sumStart + 3, 9, 10));

            // L28:Q28 -> Chất lượng sản phẩm, công việc (Ghi tổng số lượng tương ứng)
            sh1.getCell(11).setCellValue("Chất lượng sản phẩm, công việc\n(Ghi tổng số lượng tương ứng)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart, sumStart, 11, 16));

            sh2.getCell(11).setCellValue("Đảm bảo");
            sh3.getCell(11).setCellValue("(9)");
            sVal.getCell(11).setCellValue(form.getQualityStandard());

            sh2.getCell(12).setCellValue("Vượt mức (được cấp có thẩm quyền ghi nhận, biểu dương, khen thưởng)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 1, sumStart + 1, 12, 13));
            sh3.getCell(12).setCellValue("(10)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 2, sumStart + 2, 12, 13));
            sVal.getCell(12).setCellValue(form.getQualityExceeded());
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 3, sumStart + 3, 12, 13));

            sh2.getCell(14).setCellValue("Chưa đảm bảo");
            sh3.getCell(14).setCellValue("(11)");
            sVal.getCell(14).setCellValue(form.getQualitySubstandard());

            sh2.getCell(15).setCellValue("Tỷ lệ đảm bảo trở lên (%)\n(12) = (9+10)/(1) x 100");
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 1, sumStart + 1, 15, 16));
            sh3.getCell(15).setCellValue("(12)");
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 2, sumStart + 2, 15, 16));
            sVal.getCell(15).setCellValue(String.format("%.1f%%", form.getQualityRate()));
            sheet.addMergedRegion(new CellRangeAddress(sumStart + 3, sumStart + 3, 15, 16));

            // R28:R30 -> Ghi chú (13)
            sh1.getCell(17).setCellValue("Ghi chú");
            sheet.addMergedRegion(new CellRangeAddress(sumStart, sumStart + 1, 17, 17));
            sh3.getCell(17).setCellValue("(13)");
            sVal.getCell(17).setCellValue(form.getSummaryNotes() != null ? form.getSummaryNotes() : "");

            Row rBlankSig = sheet.createRow(rowIdx++);
            rBlankSig.setHeightInPoints(8f);

            // ==========================================
            // Signature Section with Digital Signature Stamps
            // ==========================================
            // Hàng ngày tháng phía trên chức danh ký
            Row rSigDate = sheet.createRow(rowIdx++);
            rSigDate.setHeightInPoints(16f);

            // Cột 13-17: CÁ NHÂN TỰ ĐÁNH GIÁ (ngày ký của giáo viên)
            Cell cDateTeacher = rSigDate.createCell(13);
            cDateTeacher.setCellValue(teacherDateLine);
            cDateTeacher.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 13, 17));

            // Cột 6-10: TỔ TRƯỞNG CHUYÊN MÔN (ngày ký của tổ trưởng nếu có)
            if (sigDeptLeader != null || result.getReviewedAt() != null) {
                LocalDate revDate = sigDeptLeader != null && sigDeptLeader.getSignedAt() != null
                        ? sigDeptLeader.getSignedAt().atZone(vnZone).toLocalDate()
                        : result.getReviewedAt().atZone(vnZone).toLocalDate();
                String revDateLine = "Cẩm Lệ, ngày " + String.format("%02d", revDate.getDayOfMonth())
                        + " tháng " + String.format("%02d", revDate.getMonthValue())
                        + " năm " + revDate.getYear();
                Cell cDateDept = rSigDate.createCell(6);
                cDateDept.setCellValue(revDateLine);
                cDateDept.setCellStyle(metaCenterItalic);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 6, 10));
            }

            // Cột 1-4: PHÓ HIỆU TRƯỞNG PHỤ TRÁCH / BAN GIÁM HIỆU (ngày ký duyệt của BGH nếu có)
            if (sigPrincipal != null || result.getBghReviewedAt() != null) {
                LocalDate bghDate = (sigPrincipal != null && sigPrincipal.getSignedAt() != null)
                        ? sigPrincipal.getSignedAt().atZone(vnZone).toLocalDate()
                        : result.getBghReviewedAt().atZone(vnZone).toLocalDate();
                String bghDateLine = "Cẩm Lệ, ngày " + String.format("%02d", bghDate.getDayOfMonth())
                        + " tháng " + String.format("%02d", bghDate.getMonthValue())
                        + " năm " + bghDate.getYear();
                Cell cDateBgh = rSigDate.createCell(1);
                cDateBgh.setCellValue(bghDateLine);
                cDateBgh.setCellStyle(metaCenterItalic);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));
            }

            Row rSigTitle = sheet.createRow(rowIdx++);
            rSigTitle.setHeightInPoints(18f);
            Cell sig1 = rSigTitle.createCell(1);
            sig1.setCellValue("PHÓ HIỆU TRƯỞNG PHỤ TRÁCH");
            sig1.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));

            Cell sig2 = rSigTitle.createCell(6);
            sig2.setCellValue("TỔ TRƯỞNG CHUYÊN MÔN");
            sig2.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 6, 10));

            Cell sig3 = rSigTitle.createCell(13);
            sig3.setCellValue("CÁ NHÂN TỰ ĐÁNH GIÁ");
            sig3.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 13, 17));

            Row rSigNote = sheet.createRow(rowIdx++);
            rSigNote.setHeightInPoints(15f);
            Cell sn1 = rSigNote.createCell(1);
            sn1.setCellValue("(Ký và ghi rõ họ tên)");
            sn1.setCellStyle(metaCenterSmallItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));

            Cell sn2 = rSigNote.createCell(6);
            sn2.setCellValue("(Ký và ghi rõ họ tên)");
            sn2.setCellStyle(metaCenterSmallItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 6, 10));

            Cell sn3 = rSigNote.createCell(13);
            sn3.setCellValue("(Ký và ghi rõ họ tên)");
            sn3.setCellStyle(metaCenterSmallItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 13, 17));

            // Khu vực dán chữ ký: chừa 4 dòng trống để dán hình ảnh chữ ký / con dấu số
            int sigImgRowStart = rowIdx;
            for (int i = 0; i < 4; i++) {
                Row rBlank = sheet.createRow(rowIdx++);
                rBlank.setHeightInPoints(16f);
            }
            int sigImgRowEnd = rowIdx;

            // Dán hình ảnh con dấu / chữ ký số (PNG) căn giữa chuẩn xác vào cột người ký, nằm ngay trên dòng Họ và Tên
            if (sigPrincipal != null) {
                byte[] img = com.school.lessonplan.nq52.signature.Nq52SignatureImageHelper.getSignaturePngBytes(
                        sigPrincipal, "Ban Giám Hiệu", "BGH", true, false);
                if (img != null) {
                    insertSignatureImageCentered(workbook, sheet, img, 1, 4, sigImgRowStart, 4);
                }
            }
            if (sigDeptLeader != null) {
                byte[] img = com.school.lessonplan.nq52.signature.Nq52SignatureImageHelper.getSignaturePngBytes(
                        sigDeptLeader, sigDeptLeader.getSignerNameSnapshot(), "Tổ trưởng chuyên môn", false, true);
                if (img != null) {
                    insertSignatureImageCentered(workbook, sheet, img, 6, 10, sigImgRowStart, 4);
                }
            }
            if (sigTeacher != null) {
                byte[] img = com.school.lessonplan.nq52.signature.Nq52SignatureImageHelper.getSignaturePngBytes(
                        sigTeacher, sigTeacher.getSignerNameSnapshot(), "Giáo viên", false, false);
                if (img != null) {
                    insertSignatureImageCentered(workbook, sheet, img, 13, 17, sigImgRowStart, 4);
                }
            }

            // Hàng in Họ và tên cuối cùng
            Row rSigNames = sheet.createRow(rowIdx++);
            if (sigPrincipal != null) {
                Cell nameCell = rSigNames.createCell(1);
                nameCell.setCellValue("Ban Giám Hiệu");
                nameCell.setCellStyle(metaCenterBold);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));
            } else if (result.getBghReviewerNameSnapshot() != null && !result.getBghReviewerNameSnapshot().isBlank()) {
                Cell nameCell = rSigNames.createCell(1);
                nameCell.setCellValue(result.getBghReviewerNameSnapshot());
                nameCell.setCellStyle(metaCenterBold);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));
            } else if (result.getReviewerNameSnapshot() != null) {
                boolean isBgh = result.getReviewer() != null && result.getReviewer().getRoles() != null
                        && result.getReviewer().getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
                if (isBgh) {
                    Cell nameCell = rSigNames.createCell(1);
                    nameCell.setCellValue(result.getReviewerNameSnapshot());
                    nameCell.setCellStyle(metaCenterBold);
                    sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));
                }
            }

            if (sigDeptLeader != null) {
                Cell nameCell = rSigNames.createCell(6);
                nameCell.setCellValue(sigDeptLeader.getSignerNameSnapshot());
                nameCell.setCellStyle(metaCenterBold);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 6, 10));
            } else if (result.getReviewerNameSnapshot() != null) {
                boolean isBgh = result.getReviewer() != null && result.getReviewer().getRoles() != null
                        && result.getReviewer().getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
                if (!isBgh) {
                    Cell nameCell = rSigNames.createCell(6);
                    nameCell.setCellValue(result.getReviewerNameSnapshot());
                    nameCell.setCellStyle(metaCenterBold);
                    sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 6, 10));
                }
            }

            Cell teacherCell = rSigNames.createCell(13);
            String teacherDisplayName = sigTeacher != null ? sigTeacher.getSignerNameSnapshot()
                    : (result.getTeacherNameSnapshot() != null ? result.getTeacherNameSnapshot() : "");
            teacherCell.setCellValue(teacherDisplayName);
            teacherCell.setCellStyle(metaCenterBold);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 13, 17));

            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi khi xuất file Excel Mẫu 01TKNV: {}", e.getMessage(), e);
            throw new RuntimeException("Có lỗi xảy ra khi tạo file Excel Mẫu 01TKNV", e);
        }
    }

    private void insertSignatureImageCentered(Workbook workbook, Sheet sheet, byte[] imageBytes, int startCol, int endCol, int startRow, int numRows) {
        if (imageBytes == null || imageBytes.length == 0) return;
        try {
            int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                drawing = sheet.createDrawingPatriarch();
            }

            // Kích thước mong muốn theo tỷ lệ cân đối (tối đa 140x48 pt)
            java.awt.Dimension dim = com.school.lessonplan.nq52.signature.Nq52SignatureImageHelper.getProportionalDimension(imageBytes, 140, 48);
            long imgWidthEmu = (long) dim.width * 12700L;

            // Tính tổng độ rộng vùng merge (startCol -> endCol) theo đơn vị EMU
            long totalRangeEmu = 0;
            long[] colWidthsEmu = new long[endCol - startCol + 1];
            for (int i = 0; i <= endCol - startCol; i++) {
                int c = startCol + i;
                float px = sheet.getColumnWidthInPixels(c);
                long colEmu = (long) (px * 9525.0);
                colWidthsEmu[i] = colEmu;
                totalRangeEmu += colEmu;
            }

            // Tính khoảng cách lề trái để căn giữa hoàn toàn theo chiều ngang vùng ký
            long leftOffsetEmu = (totalRangeEmu - imgWidthEmu) / 2;
            if (leftOffsetEmu < 0) leftOffsetEmu = 0;

            int targetCol = startCol;
            long offsetInCol = leftOffsetEmu;
            for (int i = 0; i <= endCol - startCol; i++) {
                if (offsetInCol < colWidthsEmu[i] || i == endCol - startCol) {
                    targetCol = startCol + i;
                    break;
                }
                offsetInCol -= colWidthsEmu[i];
            }

            // Tính căn giữa theo chiều dọc trong khu vực các dòng ký
            float totalRowHeightPt = numRows * 16f;
            long totalRowHeightEmu = (long) (totalRowHeightPt * 12700L);
            long imgHeightEmu = (long) dim.height * 12700L;
            long topOffsetEmu = Math.max(0, (totalRowHeightEmu - imgHeightEmu) / 2);

            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(targetCol);
            anchor.setDx1((int) offsetInCol);
            anchor.setRow1(startRow);
            anchor.setDy1((int) topOffsetEmu);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

            Picture pict = drawing.createPicture(anchor, pictureIdx);

            // Co giãn theo đúng tỷ lệ để đạt kích thước dim.width và dim.height
            java.awt.Dimension origDim = pict.getImageDimension();
            if (origDim != null && origDim.getWidth() > 0 && origDim.getHeight() > 0) {
                double scale = Math.min((double) dim.width / origDim.getWidth(), (double) dim.height / origDim.getHeight());
                pict.resize(scale, scale);
            } else {
                pict.resize(0.5, 0.5);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi chèn hình ảnh chữ ký căn giữa vào Excel: {}", e.getMessage());
        }
    }

    private String formatCaProvider(String provider) {
        if ("VNPT_SMARTCA".equalsIgnoreCase(provider)) return "VNPT SmartCA";
        if ("VIETTEL_CLOUD_CA".equalsIgnoreCase(provider)) return "Viettel Cloud-CA";
        if ("VN_EID".equalsIgnoreCase(provider)) return "VNeID (BCA)";
        if ("BAN_CO_YEU_CHINH_PHU".equalsIgnoreCase(provider)) return "Ban Cơ yếu CP";
        return provider != null ? provider : "Cloud CA";
    }

    private Row createStyledRow(Sheet sheet, int rowIdx, int cols, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        for (int c = 0; c < cols; c++) {
            Cell cell = row.createCell(c);
            cell.setCellStyle(style);
        }
        return row;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * Xuất Bảng tổng hợp đánh giá xếp loại chất lượng viên chức NQ52 theo đúng định dạng mẫu
     * - Khổ giấy A4 nằm ngang, fit gọn 1 trang in
     * - 100% phông Times New Roman
     * - 9 cột dữ liệu (A-I) kèm đánh dấu x theo xếp loại của Ban Giám Hiệu
     */
    public byte[] generateEvaluationSummaryExcel(Nq52EvaluationSummaryDto summary) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Tong_hop_xep_loai_NQ52");
            sheet.setDisplayGridlines(true);

            // Cấu hình 9 cột (A - I) tối ưu hiển thị trên khổ giấy A4 nằm ngang
            sheet.setColumnWidth(0, (int) (6.0 * 256));   // A: STT
            sheet.setColumnWidth(1, (int) (27.0 * 256));  // B: Họ và tên
            sheet.setColumnWidth(2, (int) (11.0 * 256));  // C: Năm sinh
            sheet.setColumnWidth(3, (int) (32.0 * 256));  // D: Chức vụ, chức danh
            sheet.setColumnWidth(4, (int) (16.0 * 256));  // E: Hoàn thành Xuất sắc nhiệm vụ
            sheet.setColumnWidth(5, (int) (15.0 * 256));  // F: Hoàn thành tốt nhiệm vụ
            sheet.setColumnWidth(6, (int) (15.0 * 256));  // G: Hoàn thành nhiệm vụ
            sheet.setColumnWidth(7, (int) (16.0 * 256));  // H: Không hoàn thành nhiệm vụ
            sheet.setColumnWidth(8, (int) (18.0 * 256));  // I: Ghi chú

            // =========================================================================
            // CẤU HÌNH TRANG IN (PAGE SETUP): KHỔ A4 NẰM NGANG, FIT GỌN VÀO 1 TRANG
            // =========================================================================
            PrintSetup printSetup = sheet.getPrintSetup();
            printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
            printSetup.setLandscape(true);
            sheet.setAutobreaks(true);
            sheet.setFitToPage(true);
            printSetup.setFitWidth((short) 1);
            printSetup.setFitHeight((short) 1);

            // Căn lề chuẩn trang in
            sheet.setMargin(Sheet.LeftMargin, 0.35);
            sheet.setMargin(Sheet.RightMargin, 0.35);
            sheet.setMargin(Sheet.TopMargin, 0.35);
            sheet.setMargin(Sheet.BottomMargin, 0.35);
            sheet.setMargin(Sheet.HeaderMargin, 0.15);
            sheet.setMargin(Sheet.FooterMargin, 0.15);
            sheet.setHorizontallyCenter(true);

            // =========================================================================
            // PHÔNG CHỮ: 100% PHÔNG TIMES NEW ROMAN
            // =========================================================================
            Font defaultFont = workbook.getFontAt(0);
            if (defaultFont != null) {
                defaultFont.setFontName("Times New Roman");
            }

            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Times New Roman");
            fontTitle.setFontHeightInPoints((short) 14);
            fontTitle.setBold(true);

            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontName("Times New Roman");
            fontSubtitle.setFontHeightInPoints((short) 11);
            fontSubtitle.setItalic(true);

            Font fontHeader = workbook.createFont();
            fontHeader.setFontName("Times New Roman");
            fontHeader.setFontHeightInPoints((short) 10);
            fontHeader.setBold(true);

            Font fontRegular = workbook.createFont();
            fontRegular.setFontName("Times New Roman");
            fontRegular.setFontHeightInPoints((short) 10);

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Times New Roman");
            fontBold.setFontHeightInPoints((short) 10);
            fontBold.setBold(true);

            Font fontMarkX = workbook.createFont();
            fontMarkX.setFontName("Times New Roman");
            fontMarkX.setFontHeightInPoints((short) 12);
            fontMarkX.setBold(true);

            Font fontItalic = workbook.createFont();
            fontItalic.setFontName("Times New Roman");
            fontItalic.setFontHeightInPoints((short) 10);
            fontItalic.setItalic(true);

            Font fontStampTitle = workbook.createFont();
            fontStampTitle.setFontName("Times New Roman");
            fontStampTitle.setFontHeightInPoints((short) 9);
            fontStampTitle.setBold(true);
            fontStampTitle.setColor(IndexedColors.DARK_BLUE.getIndex());

            Font fontStampBody = workbook.createFont();
            fontStampBody.setFontName("Times New Roman");
            fontStampBody.setFontHeightInPoints((short) 8);

            // Cell Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(fontTitle);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setFont(fontSubtitle);
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(fontHeader);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            setBorders(headerStyle);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle cellLeft = workbook.createCellStyle();
            cellLeft.setFont(fontRegular);
            cellLeft.setAlignment(HorizontalAlignment.LEFT);
            cellLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            cellLeft.setWrapText(true);
            setBorders(cellLeft);

            CellStyle cellCenter = workbook.createCellStyle();
            cellCenter.setFont(fontRegular);
            cellCenter.setAlignment(HorizontalAlignment.CENTER);
            cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            cellCenter.setWrapText(true);
            setBorders(cellCenter);

            CellStyle cellMarkX = workbook.createCellStyle();
            cellMarkX.setFont(fontMarkX);
            cellMarkX.setAlignment(HorizontalAlignment.CENTER);
            cellMarkX.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellMarkX);

            CellStyle cellTotalLabel = workbook.createCellStyle();
            cellTotalLabel.setFont(fontBold);
            cellTotalLabel.setAlignment(HorizontalAlignment.CENTER);
            cellTotalLabel.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellTotalLabel);

            CellStyle cellTotalCount = workbook.createCellStyle();
            cellTotalCount.setFont(fontBold);
            cellTotalCount.setAlignment(HorizontalAlignment.CENTER);
            cellTotalCount.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellTotalCount);

            // =========================================================================
            // DÒNG TIÊU ĐỀ
            // =========================================================================
            int rowIdx = 0;
            Row rTitle = sheet.createRow(rowIdx++);
            rTitle.setHeightInPoints(28);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue("KẾT QUẢ ĐÁNH GIÁ, XẾP LOẠI CHẤT LƯỢNG VIÊN CHỨC THÁNG " + summary.getMonth() + "/" + summary.getYear());
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String subtitleText = "";
            if (summary.getDepartmentName() != null && !summary.getDepartmentName().isBlank()) {
                subtitleText = "Tổ chuyên môn: " + summary.getDepartmentName();
            }
            if (summary.getCampusName() != null && !summary.getCampusName().isBlank()) {
                subtitleText += (subtitleText.isEmpty() ? "" : " - ") + "Điểm trường: " + summary.getCampusName();
            }
            if (!subtitleText.isEmpty()) {
                Row rSub = sheet.createRow(rowIdx++);
                rSub.setHeightInPoints(20);
                Cell cSub = rSub.createCell(0);
                cSub.setCellValue(subtitleText);
                cSub.setCellStyle(subtitleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));
            }

            rowIdx++; // Cách 1 dòng trống

            // =========================================================================
            // TIÊU ĐỀ BẢNG (2 TẦNG HEADER)
            // =========================================================================
            int headerRow1Idx = rowIdx++;
            int headerRow2Idx = rowIdx++;

            Row hRow1 = sheet.createRow(headerRow1Idx);
            hRow1.setHeightInPoints(30);
            Row hRow2 = sheet.createRow(headerRow2Idx);
            hRow2.setHeightInPoints(40);

            for (int c = 0; c <= 8; c++) {
                Cell c1 = hRow1.createCell(c);
                c1.setCellStyle(headerStyle);
                Cell c2 = hRow2.createCell(c);
                c2.setCellStyle(headerStyle);
            }

            // Cột 0: STT (Row1-Row2)
            hRow1.getCell(0).setCellValue("STT");
            sheet.addMergedRegion(new CellRangeAddress(headerRow1Idx, headerRow2Idx, 0, 0));

            // Cột 1: Họ và tên (Row1-Row2)
            hRow1.getCell(1).setCellValue("Họ và tên");
            sheet.addMergedRegion(new CellRangeAddress(headerRow1Idx, headerRow2Idx, 1, 1));

            // Cột 2: Năm sinh (Row1-Row2)
            hRow1.getCell(2).setCellValue("Năm sinh");
            sheet.addMergedRegion(new CellRangeAddress(headerRow1Idx, headerRow2Idx, 2, 2));

            // Cột 3: Chức vụ, chức danh (Row1-Row2)
            hRow1.getCell(3).setCellValue("Chức vụ, chức danh\n(Ghi rõ chức vụ chuyên môn, đoàn thể,\nGiáo viên bộ môn)");
            sheet.addMergedRegion(new CellRangeAddress(headerRow1Idx, headerRow2Idx, 3, 3));

            // Cột 4-7: Mức độ xếp loại chất lượng (Row1, Col 4->7)
            hRow1.getCell(4).setCellValue("Mức độ xếp loại chất lượng");
            sheet.addMergedRegion(new CellRangeAddress(headerRow1Idx, headerRow1Idx, 4, 7));

            // Row2: 4 cột con
            hRow2.getCell(4).setCellValue("Hoàn thành\nXuất sắc\nnhiệm vụ");
            hRow2.getCell(5).setCellValue("Hoàn thành\ntốt\nnhiệm vụ");
            hRow2.getCell(6).setCellValue("Hoàn thành\nnhiệm vụ");
            hRow2.getCell(7).setCellValue("Không hoàn thành\nnhiệm vụ");

            // Cột 8: Ghi chú (Row1-Row2)
            hRow1.getCell(8).setCellValue("Ghi chú");
            sheet.addMergedRegion(new CellRangeAddress(headerRow1Idx, headerRow2Idx, 8, 8));

            // =========================================================================
            // DỮ LIỆU CÁC DÒNG NHÂN SỰ
            // =========================================================================
            int countExcellent = 0;
            int countGood = 0;
            int countSatisfactory = 0;
            int countUnsatisfactory = 0;

            List<Nq52EvaluationSummaryItemDto> items = summary.getItems();
            if (items != null) {
                for (Nq52EvaluationSummaryItemDto item : items) {
                    Row dRow = sheet.createRow(rowIdx++);
                    dRow.setHeightInPoints(24);

                    Cell cellStt = dRow.createCell(0);
                    cellStt.setCellValue(item.getStt());
                    cellStt.setCellStyle(cellCenter);

                    Cell cellName = dRow.createCell(1);
                    cellName.setCellValue(item.getFullName() != null ? item.getFullName() : "");
                    cellName.setCellStyle(cellLeft);

                    Cell cellDob = dRow.createCell(2);
                    cellDob.setCellValue(item.getBirthYear() != null ? item.getBirthYear() : "");
                    cellDob.setCellStyle(cellCenter);

                    Cell cellPos = dRow.createCell(3);
                    cellPos.setCellValue(item.getPositionTitle() != null ? item.getPositionTitle() : "");
                    cellPos.setCellStyle(cellLeft);

                    Cell cellExc = dRow.createCell(4);
                    Cell cellGd = dRow.createCell(5);
                    Cell cellSat = dRow.createCell(6);
                    Cell cellUnsat = dRow.createCell(7);

                    cellExc.setCellStyle(cellMarkX);
                    cellGd.setCellStyle(cellMarkX);
                    cellSat.setCellStyle(cellMarkX);
                    cellUnsat.setCellStyle(cellMarkX);

                    if (item.isExcellent()) {
                        cellExc.setCellValue("x");
                        countExcellent++;
                    } else if (item.isGood()) {
                        cellGd.setCellValue("x");
                        countGood++;
                    } else if (item.isSatisfactory()) {
                        cellSat.setCellValue("x");
                        countSatisfactory++;
                    } else if (item.isUnsatisfactory()) {
                        cellUnsat.setCellValue("x");
                        countUnsatisfactory++;
                    }

                    Cell cellNotes = dRow.createCell(8);
                    cellNotes.setCellValue(item.getBghNotes() != null ? item.getBghNotes() : "");
                    cellNotes.setCellStyle(cellLeft);
                }
            }

            // =========================================================================
            // DÒNG TỔNG CỘNG
            // =========================================================================
            int summaryRowIdx = rowIdx++;
            Row sRow = sheet.createRow(summaryRowIdx);
            sRow.setHeightInPoints(24);

            for (int c = 0; c <= 8; c++) {
                Cell cCell = sRow.createCell(c);
                cCell.setCellStyle(cellTotalLabel);
            }

            sRow.getCell(0).setCellValue("TỔNG CỘNG");
            sheet.addMergedRegion(new CellRangeAddress(summaryRowIdx, summaryRowIdx, 0, 3));

            sRow.getCell(4).setCellValue(countExcellent > 0 ? String.valueOf(countExcellent) : "0");
            sRow.getCell(4).setCellStyle(cellTotalCount);

            sRow.getCell(5).setCellValue(countGood > 0 ? String.valueOf(countGood) : "0");
            sRow.getCell(5).setCellStyle(cellTotalCount);

            sRow.getCell(6).setCellValue(countSatisfactory > 0 ? String.valueOf(countSatisfactory) : "0");
            sRow.getCell(6).setCellStyle(cellTotalCount);

            sRow.getCell(7).setCellValue(countUnsatisfactory > 0 ? String.valueOf(countUnsatisfactory) : "0");
            sRow.getCell(7).setCellStyle(cellTotalCount);

            sRow.getCell(8).setCellValue("");

            // =========================================================================
            // CHỮ KÝ VÀ NGÀY THÁNG
            // =========================================================================
            rowIdx++;
            int dateRowIdx = rowIdx++;
            Row dtRow = sheet.createRow(dateRowIdx);
            Cell cDate = dtRow.createCell(5);
            cDate.setCellValue("......, ngày ..... tháng ..... năm " + summary.getYear());
            CellStyle italicRightStyle = workbook.createCellStyle();
            italicRightStyle.setFont(fontItalic);
            italicRightStyle.setAlignment(HorizontalAlignment.CENTER);
            cDate.setCellStyle(italicRightStyle);
            sheet.addMergedRegion(new CellRangeAddress(dateRowIdx, dateRowIdx, 5, 8));

            int signTitleIdx = rowIdx++;
            Row stRow = sheet.createRow(signTitleIdx);
            stRow.setHeightInPoints(22);

            CellStyle sigTitleStyle = workbook.createCellStyle();
            sigTitleStyle.setFont(fontBold);
            sigTitleStyle.setAlignment(HorizontalAlignment.CENTER);

            Cell cSignLeft = stRow.createCell(1);
            cSignLeft.setCellValue("NGƯỜI LẬP BIỂU");
            cSignLeft.setCellStyle(sigTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(signTitleIdx, signTitleIdx, 1, 2));

            Cell cSignRight = stRow.createCell(5);
            cSignRight.setCellValue("HIỆU TRƯỞNG");
            cSignRight.setCellStyle(sigTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(signTitleIdx, signTitleIdx, 5, 8));

            int signSubIdx = rowIdx++;
            Row subRow = sheet.createRow(signSubIdx);
            CellStyle sigSubStyle = workbook.createCellStyle();
            sigSubStyle.setFont(fontItalic);
            sigSubStyle.setAlignment(HorizontalAlignment.CENTER);

            Cell cSubLeft = subRow.createCell(1);
            cSubLeft.setCellValue("(Ký, ghi rõ họ tên)");
            cSubLeft.setCellStyle(sigSubStyle);
            sheet.addMergedRegion(new CellRangeAddress(signSubIdx, signSubIdx, 1, 2));

            Cell cSubRight = subRow.createCell(5);
            cSubRight.setCellValue("(Ký số, đóng dấu, ghi rõ họ tên)");
            cSubRight.setCellStyle(sigSubStyle);
            sheet.addMergedRegion(new CellRangeAddress(signSubIdx, signSubIdx, 5, 8));

            // Nếu Ban Giám Hiệu đã ký số, vẽ con dấu xác thực chữ ký số bên dưới
            if (summary.getBghSignedCount() > 0) {
                rowIdx++;
                int stampStart = rowIdx;
                int stampEnd = rowIdx + 3;

                CellStyle stampBoxStyle = workbook.createCellStyle();
                stampBoxStyle.setFont(fontStampBody);
                stampBoxStyle.setAlignment(HorizontalAlignment.CENTER);
                stampBoxStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                stampBoxStyle.setWrapText(true);
                stampBoxStyle.setBorderTop(BorderStyle.MEDIUM);
                stampBoxStyle.setBorderBottom(BorderStyle.MEDIUM);
                stampBoxStyle.setBorderLeft(BorderStyle.MEDIUM);
                stampBoxStyle.setBorderRight(BorderStyle.MEDIUM);
                stampBoxStyle.setTopBorderColor(IndexedColors.DARK_BLUE.getIndex());
                stampBoxStyle.setBottomBorderColor(IndexedColors.DARK_BLUE.getIndex());
                stampBoxStyle.setLeftBorderColor(IndexedColors.DARK_BLUE.getIndex());
                stampBoxStyle.setRightBorderColor(IndexedColors.DARK_BLUE.getIndex());
                stampBoxStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
                stampBoxStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int r = stampStart; r <= stampEnd; r++) {
                    Row stampRow = sheet.createRow(r);
                    for (int c = 5; c <= 8; c++) {
                        Cell cBox = stampRow.createCell(c);
                        cBox.setCellStyle(stampBoxStyle);
                    }
                }

                Row r1 = sheet.getRow(stampStart);
                r1.getCell(5).setCellValue("ĐÃ KÝ SỐ ĐIỆN TỬ BAN GIÁM HIỆU");
                CellStyle r1Style = workbook.createCellStyle();
                r1Style.cloneStyleFrom(stampBoxStyle);
                r1Style.setFont(fontStampTitle);
                r1.getCell(5).setCellStyle(r1Style);

                Row r2 = sheet.getRow(stampStart + 1);
                String signer = summary.getBghSignerName() != null ? summary.getBghSignerName() : "Ban Giám Hiệu";
                r2.getCell(5).setCellValue("Người ký: " + signer);

                Row r3 = sheet.getRow(stampStart + 2);
                String signedAt = summary.getBghSignedAtFormatted() != null ? summary.getBghSignedAtFormatted() : "";
                r3.getCell(5).setCellValue(signedAt.isEmpty() ? "Hợp lệ & Toàn vẹn" : "Thời gian ký: " + signedAt);

                Row r4 = sheet.getRow(stampStart + 3);
                r4.getCell(5).setCellValue("Chứng thư số: Hợp lệ & Toàn vẹn");

                for (int r = stampStart; r <= stampEnd; r++) {
                    sheet.addMergedRegion(new CellRangeAddress(r, r, 5, 8));
                }
            }

            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi xuất file Excel tổng hợp đánh giá NQ52: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo file Excel tổng hợp NQ52: " + e.getMessage(), e);
        }
    }
}
