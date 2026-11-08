package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52WorkResultForm;
import com.school.lessonplan.nq52.dto.Nq52WorkResultTaskItem;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class Nq52WorkResultControllerTest {

    private Nq52WorkResultService workResultService;
    private UserRepository userRepository;
    private Nq52WorkResultController controller;
    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    public void setup() {
        workResultService = Mockito.mock(Nq52WorkResultService.class);
        userRepository = Mockito.mock(UserRepository.class);
        com.school.lessonplan.campus.CampusRepository campusRepository = Mockito.mock(com.school.lessonplan.campus.CampusRepository.class);
        com.school.lessonplan.department.DepartmentRepository departmentRepository = Mockito.mock(com.school.lessonplan.department.DepartmentRepository.class);
        com.school.lessonplan.nq52.signature.Nq52SignatureService signatureService = Mockito.mock(com.school.lessonplan.nq52.signature.Nq52SignatureService.class);

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("giaovien@school.edu.vn");
        testUser.setLastName("Nguyễn");
        testUser.setGivenName("Văn An");

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        controller = new Nq52WorkResultController(workResultService, userRepository, departmentRepository, campusRepository, signatureService);

        HandlerMethodArgumentResolver currentUserResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CurrentUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new CurrentUser(testUser, Collections.emptySet());
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(currentUserResolver)
                .build();
    }

    @Test
    public void testSaveWorkResult_BindsTasksSuccessfully() throws Exception {
        Nq52WorkResult savedWr = new Nq52WorkResult();
        savedWr.setId(10008L);
        savedWr.setEvaluationMonth(11);
        savedWr.setEvaluationYear(2026);

        when(workResultService.saveWorkResult(eq(10008L), any(Nq52WorkResultForm.class), any(User.class), eq(false)))
                .thenAnswer(inv -> {
                    Nq52WorkResultForm form = inv.getArgument(1);
                    assertNotNull(form.getTasks());
                    assertEquals(2, form.getTasks().size());
                    assertEquals("Soạn giáo án tuần 10", form.getTasks().get(0).getContent());
                    assertEquals("I", form.getTasks().get(0).getCategory());
                    assertEquals("NORMAL", form.getTasks().get(0).getNature());
                    assertEquals("Dạy chuyên đề đổi mới", form.getTasks().get(1).getContent());
                    assertEquals("II", form.getTasks().get(1).getCategory());
                    return savedWr;
                });

        mockMvc.perform(post("/nq52/work-results/10008/save")
                        .param("action", "SAVE")
                        .param("positionTitle", "Giáo viên Toán")
                        .param("evaluationDateStr", "Cẩm Lệ, ngày 04 tháng 11 năm 2026")
                        .param("tasks[0].id", "task_1")
                        .param("tasks[0].category", "I")
                        .param("tasks[0].orderNum", "1")
                        .param("tasks[0].content", "Soạn giáo án tuần 10")
                        .param("tasks[0].nature", "NORMAL")
                        .param("tasks[0].role", "LEAD")
                        .param("tasks[0].status", "COMPLETED")
                        .param("tasks[0].timeRequired", "01/11 - 07/11")
                        .param("tasks[0].timeProgress", "ON_TIME")
                        .param("tasks[0].quality", "STANDARD")
                        .param("tasks[1].id", "task_2")
                        .param("tasks[1].category", "II")
                        .param("tasks[1].orderNum", "1")
                        .param("tasks[1].content", "Dạy chuyên đề đổi mới")
                        .param("tasks[1].nature", "COMPLEX")
                        .param("tasks[1].role", "COORDINATE")
                        .param("tasks[1].status", "COMPLETED")
                        .param("tasks[1].timeRequired", "15/11")
                        .param("tasks[1].timeProgress", "EARLY")
                        .param("tasks[1].quality", "EXCEEDED")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nq52/work-results/10008"));
    }

    @Test
    public void testSaveWorkResult_ActionExport_RedirectsToExport() throws Exception {
        Nq52WorkResult savedWr = new Nq52WorkResult();
        savedWr.setId(10008L);
        savedWr.setEvaluationMonth(11);
        savedWr.setEvaluationYear(2026);

        when(workResultService.saveWorkResult(eq(10008L), any(Nq52WorkResultForm.class), any(User.class), eq(false)))
                .thenReturn(savedWr);

        mockMvc.perform(post("/nq52/work-results/10008/save")
                        .param("action", "EXPORT")
                        .param("tasks[0].id", "task_1")
                        .param("tasks[0].category", "I")
                        .param("tasks[0].content", "Soạn bài giảng số học 9")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nq52/work-results/10008/export"));
    }

    @Test
    public void testExcelExportService_GeneratesTasksCorrectly() throws Exception {
        Nq52ExcelExportService exportService = new Nq52ExcelExportService();
        Nq52WorkResult wr = new Nq52WorkResult();
        wr.setId(10008L);
        wr.setEvaluationMonth(11);
        wr.setEvaluationYear(2026);
        wr.setTeacherNameSnapshot("Nguyễn Văn An");

        Nq52WorkResultForm form = new Nq52WorkResultForm();
        form.setTasks(java.util.List.of(
                new Nq52WorkResultTaskItem("task_1", "I", 1, "Soạn bài giảng số học 9"),
                new Nq52WorkResultTaskItem("task_2", "II", 1, "Bồi dưỡng học sinh giỏi Toán")
        ));
        form.setTotalAssigned(2);
        form.setTotalCompleted(2);
        form.setCompletionRate(100.0);

        byte[] bytes = exportService.generateWorkResultExcel(wr, form);
        assertNotNull(bytes);
        assertTrue(bytes.length > 1000);

        // Verify sheet content
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheet("Mau_01TKNV");
            assertNotNull(sheet);
            boolean foundTask1 = false;
            boolean foundTask2 = false;
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        String val = cell.getStringCellValue();
                        if (val.contains("Soạn bài giảng số học 9")) foundTask1 = true;
                        if (val.contains("Bồi dưỡng học sinh giỏi Toán")) foundTask2 = true;
                    }
                }
            }
            assertTrue(foundTask1, "Task 1 phải xuất hiện trong file Excel");
            assertTrue(foundTask2, "Task 2 phải xuất hiện trong file Excel");
        }
    }
}

