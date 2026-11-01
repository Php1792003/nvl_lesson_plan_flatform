package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
import com.school.lessonplan.nq52.dto.Nq52WorkResultTaskItem;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
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
}
