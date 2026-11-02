package com.school.lessonplan.nq52;

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

            // Set 18 Column Widths (A - R)
            sheet.setColumnWidth(0, (int) (5.5 * 256));   // A: TT
            sheet.setColumnWidth(1, (int) (42.0 * 256));  // B: Nội dung công việc
            sheet.setColumnWidth(2, (int) (8.0 * 256));   // C: Bình thường / (1) Được giao
            sheet.setColumnWidth(3, (int) (8.0 * 256));   // D: Phức tạp / (2) Hoàn thành
            sheet.setColumnWidth(4, (int) (9.0 * 256));   // E: Rất phức tạp / (3) Chưa hoàn thành
            sheet.setColumnWidth(5, (int) (11.0 * 256));  // F: Chủ trì / (4) Tỷ lệ HT
            sheet.setColumnWidth(6, (int) (9.0 * 256));   // G: Phối hợp / (5) Sớm hạn
            sheet.setColumnWidth(7, (int) (10.0 * 256));  // H: Đã HT / (6) Đúng hạn
            sheet.setColumnWidth(8, (int) (10.0 * 256));  // I: Chưa HT / (7) Trễ hạn
            sheet.setColumnWidth(9, (int) (12.0 * 256));  // J: TG yêu cầu / (8) Tỷ lệ đúng hạn part 1
            sheet.setColumnWidth(10, (int) (8.5 * 256));  // K: Sớm hạn / (8) Tỷ lệ đúng hạn part 2
            sheet.setColumnWidth(11, (int) (9.0 * 256));  // L: Đúng hạn / (9) Đảm bảo
            sheet.setColumnWidth(12, (int) (9.0 * 256));  // M: Trễ hạn / (10) Vượt mức part 1
            sheet.setColumnWidth(13, (int) (11.0 * 256)); // N: Chưa đảm bảo / (10) Vượt mức part 2
            sheet.setColumnWidth(14, (int) (11.0 * 256)); // O: Đảm bảo / (11) Chưa đảm bảo
            sheet.setColumnWidth(15, (int) (11.0 * 256)); // P: Vượt mức / (12) Tỷ lệ chất lượng part 1
            sheet.setColumnWidth(16, (int) (12.0 * 256)); // Q: Kiến nghị / (12) Tỷ lệ chất lượng part 2
            sheet.setColumnWidth(17, (int) (16.0 * 256)); // R: Giải trình / (13) Ghi chú

            // Fonts
            Font fontRegular = workbook.createFont();
            fontRegular.setFontName("Times New Roman");
            fontRegular.setFontHeightInPoints((short) 11);

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Times New Roman");
            fontBold.setFontHeightInPoints((short) 11);
            fontBold.setBold(true);

            Font fontItalic = workbook.createFont();
            fontItalic.setFontName("Times New Roman");
            fontItalic.setFontHeightInPoints((short) 11);
            fontItalic.setItalic(true);

            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Times New Roman");
            fontTitle.setFontHeightInPoints((short) 13);
            fontTitle.setBold(true);

            // Cell Styles
            CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(fontBold);
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

            // Row 3: Date
            Row r3 = sheet.createRow(rowIdx++);
            Cell c3 = r3.createCell(7);
            String dateStr = (result.getEvaluationDateStr() != null && !result.getEvaluationDateStr().isBlank())
                    ? result.getEvaluationDateStr()
                    : "Cẩm Lệ, ngày 30 tháng " + result.getEvaluationMonth() + " năm " + result.getEvaluationYear();
            c3.setCellValue(dateStr);
            c3.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 7, 17));

            rowIdx++; // Blank row

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
            cNote.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 17));

            rowIdx++; // Blank row

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

            rowIdx++; // Blank row

            // ==========================================
            // Main Table Header Rows (Multi-level 3 rows)
            // ==========================================
            int headerStart = rowIdx;
            Row h1 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row h2 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);
            Row h3 = createStyledRow(sheet, rowIdx++, 18, tableHeaderStyle);

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

                List<Nq52WorkResultTaskItem> catTasks = groupedTasks.get(cat);
                if (catTasks != null && !catTasks.isEmpty()) {
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
                }
            }

            rowIdx++; // Blank row

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

            rowIdx += 2; // Blank rows

            // ==========================================
            // Signature Section with Digital Signature Stamps
            // ==========================================
            Row rSigTitle = sheet.createRow(rowIdx++);
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
            Cell sn1 = rSigNote.createCell(1);
            sn1.setCellValue("(Ký và ghi rõ họ tên)");
            sn1.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 4));

            Cell sn2 = rSigNote.createCell(6);
            sn2.setCellValue("(Ký và ghi rõ họ tên)");
            sn2.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 6, 10));

            Cell sn3 = rSigNote.createCell(13);
            sn3.setCellValue("(Ký và ghi rõ họ tên)");
            sn3.setCellStyle(metaCenterItalic);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 13, 17));

            // Xác định chữ ký số của từng bên
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

            // Tạo các Style cho con dấu chữ ký số điện tử trong Excel
            Font fontStampBghHead = workbook.createFont();
            fontStampBghHead.setFontName("Times New Roman");
            fontStampBghHead.setFontHeightInPoints((short) 9);
            fontStampBghHead.setBold(true);
            fontStampBghHead.setColor(IndexedColors.DARK_RED.getIndex());

            Font fontStampBghBody = workbook.createFont();
            fontStampBghBody.setFontName("Times New Roman");
            fontStampBghBody.setFontHeightInPoints((short) 8);
            fontStampBghBody.setColor(IndexedColors.RED.getIndex());

            CellStyle styleStampBghHead = workbook.createCellStyle();
            styleStampBghHead.setFont(fontStampBghHead);
            styleStampBghHead.setAlignment(HorizontalAlignment.CENTER);
            styleStampBghHead.setVerticalAlignment(VerticalAlignment.CENTER);
            styleStampBghHead.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            styleStampBghHead.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleStampBghHead);

            CellStyle styleStampBghBody = workbook.createCellStyle();
            styleStampBghBody.setFont(fontStampBghBody);
            styleStampBghBody.setAlignment(HorizontalAlignment.CENTER);
            styleStampBghBody.setVerticalAlignment(VerticalAlignment.CENTER);
            styleStampBghBody.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            styleStampBghBody.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleStampBghBody);

            Font fontStampDeptHead = workbook.createFont();
            fontStampDeptHead.setFontName("Times New Roman");
            fontStampDeptHead.setFontHeightInPoints((short) 9);
            fontStampDeptHead.setBold(true);
            fontStampDeptHead.setColor(IndexedColors.DARK_GREEN.getIndex());

            Font fontStampDeptBody = workbook.createFont();
            fontStampDeptBody.setFontName("Times New Roman");
            fontStampDeptBody.setFontHeightInPoints((short) 8);
            fontStampDeptBody.setColor(IndexedColors.GREEN.getIndex());

            CellStyle styleStampDeptHead = workbook.createCellStyle();
            styleStampDeptHead.setFont(fontStampDeptHead);
            styleStampDeptHead.setAlignment(HorizontalAlignment.CENTER);
            styleStampDeptHead.setVerticalAlignment(VerticalAlignment.CENTER);
            styleStampDeptHead.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            styleStampDeptHead.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleStampDeptHead);

            CellStyle styleStampDeptBody = workbook.createCellStyle();
            styleStampDeptBody.setFont(fontStampDeptBody);
            styleStampDeptBody.setAlignment(HorizontalAlignment.CENTER);
            styleStampDeptBody.setVerticalAlignment(VerticalAlignment.CENTER);
            styleStampDeptBody.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            styleStampDeptBody.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleStampDeptBody);

            Font fontStampTeacherHead = workbook.createFont();
            fontStampTeacherHead.setFontName("Times New Roman");
            fontStampTeacherHead.setFontHeightInPoints((short) 9);
            fontStampTeacherHead.setBold(true);
            fontStampTeacherHead.setColor(IndexedColors.DARK_BLUE.getIndex());

            Font fontStampTeacherBody = workbook.createFont();
            fontStampTeacherBody.setFontName("Times New Roman");
            fontStampTeacherBody.setFontHeightInPoints((short) 8);
            fontStampTeacherBody.setColor(IndexedColors.BLUE.getIndex());

            CellStyle styleStampTeacherHead = workbook.createCellStyle();
            styleStampTeacherHead.setFont(fontStampTeacherHead);
            styleStampTeacherHead.setAlignment(HorizontalAlignment.CENTER);
            styleStampTeacherHead.setVerticalAlignment(VerticalAlignment.CENTER);
            styleStampTeacherHead.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            styleStampTeacherHead.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleStampTeacherHead);

            CellStyle styleStampTeacherBody = workbook.createCellStyle();
            styleStampTeacherBody.setFont(fontStampTeacherBody);
            styleStampTeacherBody.setAlignment(HorizontalAlignment.CENTER);
            styleStampTeacherBody.setVerticalAlignment(VerticalAlignment.CENTER);
            styleStampTeacherBody.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            styleStampTeacherBody.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleStampTeacherBody);

            // Dán con dấu chữ ký số điện tử trực tiếp vào đúng cột người ký
            if (sigTeacher != null || sigDeptLeader != null || sigPrincipal != null) {
                // Hàng 1: Tiêu đề con dấu
                Row rStamp1 = sheet.createRow(rowIdx++);
                if (sigPrincipal != null) {
                    setStampCell(sheet, rStamp1, 1, 4, "🏛️ ĐÃ KÝ SỐ DUYỆT BGH", styleStampBghHead);
                }
                if (sigDeptLeader != null) {
                    setStampCell(sheet, rStamp1, 6, 10, "✍️ ĐÃ KÝ SỐ THẨM ĐỊNH TỔ", styleStampDeptHead);
                }
                if (sigTeacher != null) {
                    setStampCell(sheet, rStamp1, 13, 17, "✍️ ĐÃ KÝ SỐ ĐIỆN TỬ", styleStampTeacherHead);
                }

                // Hàng 2: Họ tên người ký / Đại diện
                Row rStamp2 = sheet.createRow(rowIdx++);
                if (sigPrincipal != null) {
                    setStampCell(sheet, rStamp2, 1, 4, "TRƯỜNG THCS NGUYỄN VĂN LINH", styleStampBghHead);
                }
                if (sigDeptLeader != null) {
                    setStampCell(sheet, rStamp2, 6, 10, "Ký bởi: " + sigDeptLeader.getSignerNameSnapshot(), styleStampDeptHead);
                }
                if (sigTeacher != null) {
                    setStampCell(sheet, rStamp2, 13, 17, "Ký bởi: " + sigTeacher.getSignerNameSnapshot(), styleStampTeacherHead);
                }

                // Hàng 3: Thời gian ký TSA & Dịch vụ CA
                Row rStamp3 = sheet.createRow(rowIdx++);
                if (sigPrincipal != null) {
                    setStampCell(sheet, rStamp3, 1, 4, "Thời gian: " + SIG_DATE_FMT.format(sigPrincipal.getSignedAt()) + " | USB Token", styleStampBghBody);
                }
                if (sigDeptLeader != null) {
                    setStampCell(sheet, rStamp3, 6, 10, "Thời gian: " + SIG_DATE_FMT.format(sigDeptLeader.getSignedAt()) + " | " + formatCaProvider(sigDeptLeader.getCaProvider()), styleStampDeptBody);
                }
                if (sigTeacher != null) {
                    setStampCell(sheet, rStamp3, 13, 17, "Thời gian: " + SIG_DATE_FMT.format(sigTeacher.getSignedAt()) + " | " + formatCaProvider(sigTeacher.getCaProvider()), styleStampTeacherBody);
                }

                // Hàng 4: Xác thực Realtime & Toàn vẹn dữ liệu
                Row rStamp4 = sheet.createRow(rowIdx++);
                if (sigPrincipal != null) {
                    setStampCell(sheet, rStamp4, 1, 4, "✓ Phê duyệt chính thức & Ban hành hợp lệ", styleStampBghBody);
                }
                if (sigDeptLeader != null) {
                    setStampCell(sheet, rStamp4, 6, 10, "✓ Xác thực Realtime: Hợp lệ & Toàn vẹn (SHA-256)", styleStampDeptBody);
                }
                if (sigTeacher != null) {
                    setStampCell(sheet, rStamp4, 13, 17, "✓ Xác thực Realtime: Hợp lệ & Toàn vẹn (SHA-256)", styleStampTeacherBody);
                }

                rowIdx += 1; // Khoảng cách nhỏ trước họ tên
            } else {
                rowIdx += 3; // Để trống nếu chưa bên nào ký
            }

            // Hàng in Họ và tên cuối cùng
            Row rSigNames = sheet.createRow(rowIdx++);
            if (sigPrincipal != null) {
                Cell nameCell = rSigNames.createCell(1);
                nameCell.setCellValue("Ban Giám Hiệu");
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

    private void setStampCell(Sheet sheet, Row row, int startCol, int endCol, String text, CellStyle style) {
        for (int c = startCol; c <= endCol; c++) {
            Cell cell = row.createCell(c);
            cell.setCellStyle(style);
        }
        Cell first = row.getCell(startCol);
        if (first != null && text != null) {
            first.setCellValue(text);
        }
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), startCol, endCol));
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
}
