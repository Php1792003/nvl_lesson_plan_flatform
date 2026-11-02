package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.dto.Nq52EvaluationForm;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class Nq52DocxExportService {

    private static final Logger log = LoggerFactory.getLogger(Nq52DocxExportService.class);
    private static final DecimalFormat SCORE_FMT = new DecimalFormat("#,##0.##");

    private record RowMapping(int tableIdx, int rowIdx, String key, RowType type) {}
    private enum RowType {
        HEADER,
        SUBTOTAL,
        CRITERIA,
        SECTION_EMPTY
    }

    private static final List<RowMapping> ROW_MAPPINGS = List.of(
        // Table 0
        new RowMapping(0, 0, "HEADER", RowType.HEADER),
        new RowMapping(0, 1, "A", RowType.SUBTOTAL),
        new RowMapping(0, 2, "A_I", RowType.SUBTOTAL),
        new RowMapping(0, 3, "A_I_1", RowType.SUBTOTAL),
        new RowMapping(0, 4, "A_I_1_a", RowType.CRITERIA),
        new RowMapping(0, 5, "A_I_1_b", RowType.CRITERIA),
        new RowMapping(0, 6, "A_I_1_c", RowType.CRITERIA),
        new RowMapping(0, 7, "A_I_1_d", RowType.CRITERIA),
        new RowMapping(0, 8, "A_I_1_dd", RowType.CRITERIA),
        new RowMapping(0, 9, "A_I_1_e", RowType.CRITERIA),
        new RowMapping(0, 10, "A_I_1_ee", RowType.CRITERIA),
        // Table 1
        new RowMapping(1, 0, "A_I_1_g", RowType.CRITERIA),
        new RowMapping(1, 1, "A_I_2", RowType.SUBTOTAL),
        new RowMapping(1, 2, "A_I_2_a", RowType.CRITERIA),
        new RowMapping(1, 3, "A_I_2_b", RowType.CRITERIA),
        new RowMapping(1, 4, "A_I_2_c", RowType.CRITERIA),
        new RowMapping(1, 5, "A_I_2_d", RowType.CRITERIA),
        new RowMapping(1, 6, "A_II", RowType.SUBTOTAL),
        new RowMapping(1, 7, "A_II_1", RowType.SUBTOTAL),
        new RowMapping(1, 8, "A_II_1_a", RowType.CRITERIA),
        new RowMapping(1, 9, "A_II_1_b", RowType.CRITERIA),
        new RowMapping(1, 10, "A_II_1_c", RowType.CRITERIA),
        new RowMapping(1, 11, "A_II_2", RowType.SUBTOTAL),
        new RowMapping(1, 12, "A_II_2_a", RowType.CRITERIA),
        new RowMapping(1, 13, "A_II_2_b", RowType.CRITERIA),
        new RowMapping(1, 14, "A_II_3", RowType.SUBTOTAL),
        new RowMapping(1, 15, "A_II_3_a", RowType.CRITERIA),
        new RowMapping(1, 16, "A_II_3_b", RowType.CRITERIA),
        new RowMapping(1, 17, "A_II_4", RowType.SUBTOTAL),
        new RowMapping(1, 18, "A_II_4_a", RowType.CRITERIA),
        new RowMapping(1, 19, "A_II_4_b", RowType.CRITERIA),
        // Table 2
        new RowMapping(2, 0, "A_III", RowType.SUBTOTAL),
        new RowMapping(2, 1, "A_III_1", RowType.CRITERIA),
        new RowMapping(2, 2, "A_III_2", RowType.CRITERIA),
        new RowMapping(2, 3, "A_III_3", RowType.CRITERIA),
        new RowMapping(2, 4, "A_III_4", RowType.CRITERIA),
        new RowMapping(2, 5, "B", RowType.SUBTOTAL),
        new RowMapping(2, 6, "B_1", RowType.SUBTOTAL),
        new RowMapping(2, 7, "B_1_a", RowType.CRITERIA),
        new RowMapping(2, 8, "B_1_b", RowType.CRITERIA),
        new RowMapping(2, 9, "B_1_c", RowType.CRITERIA),
        new RowMapping(2, 10, "B_1_d", RowType.CRITERIA),
        new RowMapping(2, 11, "B_1_dd", RowType.CRITERIA),
        new RowMapping(2, 12, "B_1_e", RowType.CRITERIA),
        new RowMapping(2, 13, "B_2", RowType.SUBTOTAL),
        new RowMapping(2, 14, "B_2_a", RowType.CRITERIA),
        new RowMapping(2, 15, "B_2_b", RowType.CRITERIA),
        new RowMapping(2, 16, "B_2_c", RowType.CRITERIA),
        new RowMapping(2, 17, "B_2_d", RowType.CRITERIA),
        // Table 3
        new RowMapping(3, 0, "B_2_dd", RowType.CRITERIA),
        new RowMapping(3, 1, "B_2_e", RowType.CRITERIA),
        new RowMapping(3, 2, "B_3", RowType.SUBTOTAL),
        new RowMapping(3, 3, "B_3_a", RowType.CRITERIA),
        new RowMapping(3, 4, "B_3_b", RowType.CRITERIA),
        new RowMapping(3, 5, "B_3_c", RowType.CRITERIA),
        new RowMapping(3, 6, "B_3_d", RowType.CRITERIA),
        new RowMapping(3, 7, "B_3_dd", RowType.CRITERIA),
        new RowMapping(3, 8, "B_3_e", RowType.CRITERIA),
        new RowMapping(3, 9, "B_3_g", RowType.CRITERIA),
        new RowMapping(3, 10, "A_PLUS_B", RowType.SUBTOTAL),
        new RowMapping(3, 11, "C", RowType.SECTION_EMPTY),
        new RowMapping(3, 12, "C_I", RowType.SECTION_EMPTY),
        new RowMapping(3, 13, "C_I_1", RowType.CRITERIA),
        new RowMapping(3, 14, "C_I_2", RowType.CRITERIA),
        new RowMapping(3, 15, "C_I_3", RowType.CRITERIA),
        new RowMapping(3, 16, "C_I_4", RowType.CRITERIA),
        new RowMapping(3, 17, "C_II", RowType.SECTION_EMPTY),
        // Table 4
        new RowMapping(4, 0, "C_II_1", RowType.CRITERIA),
        new RowMapping(4, 1, "C_II_2", RowType.CRITERIA),
        new RowMapping(4, 2, "C_II_3", RowType.CRITERIA)
    );

    /**
     * Tạo file Word Mẫu 03 (.docx) từ template chính thức và nạp dữ liệu đánh giá
     */
    public byte[] generateM03Docx(Nq52Evaluation eval, Nq52EvaluationForm form) {
        try {
            ClassPathResource templateResource = new ClassPathResource("templates/nq52/template_mau_03.docx");
            try (InputStream is = templateResource.getInputStream();
                 XWPFDocument doc = new XWPFDocument(is)) {

                // 1. Điền các trường thông tin chung và phần tổng hợp đánh giá
                fillHeaderAndSummaryParagraphs(doc, eval, form);

                // 2. Điền điểm vào các bảng tiêu chí (Tables 0 đến 4)
                fillCriteriaTables(doc, eval, form);

                // 3. Đánh dấu kết quả xếp loại vào Table 5
                fillClassificationCheckbox(doc, eval);

                // 4. Xuất file dạng byte array
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo file Word Mẫu 03 cho evaluation id={}", eval.getId(), e);
            throw new RuntimeException("Không thể tạo file Word Mẫu 03: " + e.getMessage(), e);
        }
    }

    private void fillHeaderAndSummaryParagraphs(XWPFDocument doc, Nq52Evaluation eval, Nq52EvaluationForm form) {
        String evalPeriod = "Kỳ đánh giá: Tháng " + eval.getEvaluationMonth() + " / Năm " + eval.getEvaluationYear();
        String teacherName = "Họ và tên: " + eval.getTeacherNameSnapshot();
        String position = "Chức vụ, chức danh: " + (eval.getPositionTitle() != null ? eval.getPositionTitle() : "Giáo viên");
        String unit = "Đơn vị công tác: " + eval.getWorkUnit() + " — Tổ chuyên môn: " + eval.getDepartmentNameSnapshot();

        BigDecimal finalScoreA = eval.getBghScoreA() != null ? eval.getBghScoreA() : (eval.getStatus() == Nq52Status.REVIEWED && eval.getReviewerScoreA() != null ? eval.getReviewerScoreA() : eval.getSelfScoreA());
        BigDecimal finalScoreB = eval.getBghScoreB() != null ? eval.getBghScoreB() : (eval.getStatus() == Nq52Status.REVIEWED && eval.getReviewerScoreB() != null ? eval.getReviewerScoreB() : eval.getSelfScoreB());
        BigDecimal finalTotal = eval.getBghTotalScore() != null ? eval.getBghTotalScore() : (eval.getStatus() == Nq52Status.REVIEWED && eval.getReviewerTotalScore() != null ? eval.getReviewerTotalScore() : eval.getSelfTotalScore());
        BigDecimal finalCPlus = eval.getBghScoreCPlus() != null ? eval.getBghScoreCPlus() : (eval.getStatus() == Nq52Status.REVIEWED && eval.getReviewerScoreCPlus() != null ? eval.getReviewerScoreCPlus() : eval.getSelfScoreCPlus());
        BigDecimal finalCMinus = eval.getBghScoreCMinus() != null ? eval.getBghScoreCMinus() : (eval.getStatus() == Nq52Status.REVIEWED && eval.getReviewerScoreCMinus() != null ? eval.getReviewerScoreCMinus() : eval.getSelfScoreCMinus());

        String scoreAStr = "Điểm tiêu chí chung: " + formatScore(finalScoreA) + " điểm";
        String scoreBStr = "Điểm tiêu chí kết quả thực hiện nhiệm vụ: " + formatScore(finalScoreB) + " điểm";
        String totalScoreStr = "Tổng điểm theo dõi, đánh giá chính thức: " + formatScore(finalTotal) + " điểm";

        String cPlusStr = "Tổng điểm cộng (nếu có): " + (finalCPlus != null && finalCPlus.compareTo(BigDecimal.ZERO) > 0 ? ("+" + formatScore(finalCPlus)) : "0") + " điểm";
        String cMinusStr = "Tổng điểm trừ (nếu có): " + (finalCMinus != null && finalCMinus.compareTo(BigDecimal.ZERO) > 0 ? ("-" + formatScore(finalCMinus)) : "0") + " điểm";

        String strengths = "Ưu điểm: " + (eval.getStrengths() != null && !eval.getStrengths().isBlank() ? eval.getStrengths() : "Hoàn thành tốt nhiệm vụ giảng dạy và công tác được giao.");
        String weaknesses = "Hạn chế, khuyết điểm: " + (eval.getWeaknesses() != null && !eval.getWeaknesses().isBlank() ? eval.getWeaknesses() : "Không");

        LocalDate today = LocalDate.now();
        String dateLine = "Cẩm Lệ, ngày " + String.format("%02d", today.getDayOfMonth()) + " tháng " + String.format("%02d", today.getMonthValue()) + " năm " + today.getYear();

        for (int i = 0; i < doc.getParagraphs().size(); i++) {
            XWPFParagraph p = doc.getParagraphs().get(i);
            String text = p.getText();

            if (text.contains("Kỳ đánh giá: Tháng")) {
                replaceParagraphText(p, evalPeriod, false, false, ParagraphAlignment.CENTER);
            } else if (text.startsWith("Họ và tên:")) {
                replaceParagraphText(p, teacherName, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Chức vụ, chức danh:")) {
                replaceParagraphText(p, position, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Đơn vị công tác:")) {
                replaceParagraphText(p, unit, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Điểm tiêu chí chung:")) {
                replaceParagraphText(p, scoreAStr, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Điểm tiêu chí kết quả thực hiện nhiệm vụ:")) {
                replaceParagraphText(p, scoreBStr, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Tổng điểm theo dõi, đánh giá chính thức:")) {
                replaceParagraphText(p, totalScoreStr, true, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Tổng điểm cộng (nếu có):")) {
                replaceParagraphText(p, cPlusStr, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Tổng điểm trừ (nếu có):")) {
                replaceParagraphText(p, cMinusStr, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Ưu điểm:")) {
                replaceParagraphText(p, strengths, false, false, ParagraphAlignment.LEFT);
            } else if (text.startsWith("Hạn chế, khuyết điểm:")) {
                replaceParagraphText(p, weaknesses, false, false, ParagraphAlignment.LEFT);
            } else if (text.contains("Cẩm Lệ, ngày 01 tháng 11  năm 2026") || text.contains("Cẩm Lệ, ngày 01 tháng 11 năm 2026")) {
                replaceParagraphText(p, dateLine, false, true, p.getAlignment());
            } else if (text.contains("* Đề xuất mức xếp loại chất lượng:")) {
                String revClass = eval.getReviewerClassification();
                if (revClass != null && !revClass.isBlank()) {
                    replaceParagraphText(p, "* Đề xuất mức xếp loại chất lượng: " + revClass, true, false, ParagraphAlignment.LEFT);
                } else {
                    replaceParagraphText(p, "* Đề xuất mức xếp loại chất lượng: ..............................................................", false, false, ParagraphAlignment.LEFT);
                }
                p.setIndentationLeft(994);
                p.setIndentationRight(0);
                p.setIndentationFirstLine(0);
                p.setIndentationHanging(0);
            } else if (text.contains("NGƯỜI TỰ ĐÁNH GIÁ")) {
                insertTeacherSignatureName(doc, i, eval.getTeacherNameSnapshot());
            } else if (text.contains("NGƯỜI CÓ THẨM QUYỀN QUẢN LÝ TRỰC TIẾP ĐÁNH GIÁ")) {
                if (eval.getReviewerNameSnapshot() != null && !eval.getReviewerNameSnapshot().isBlank()) {
                    insertReviewerSignatureName(doc, i, eval.getReviewerNameSnapshot());
                }
            }
        }
    }

    private void fillCriteriaTables(XWPFDocument doc, Nq52Evaluation eval, Nq52EvaluationForm form) {
        List<XWPFTable> tables = doc.getTables();
        if (tables.size() < 5) {
            log.warn("Document contains less than 5 tables, skipping criteria table filling");
            return;
        }

        Map<String, Double> selfScores = form.getSelfScores();
        Map<String, Double> revScores = form.getReviewerScores();
        Map<String, Double> bghScores = form.getBghScores();
        Map<String, String> explanations = form.getExplanations();
        boolean hasReviewer = eval.getStatus() == Nq52Status.REVIEWED || form.hasReviewerData();
        boolean hasBgh = eval.getStatus() == Nq52Status.COUNCIL_REVIEWED || form.hasBghData() || eval.getBghTotalScore() != null;

        for (RowMapping rm : ROW_MAPPINGS) {
            if (rm.tableIdx() >= tables.size()) continue;
            XWPFTable tbl = tables.get(rm.tableIdx());
            if (rm.rowIdx() >= tbl.getRows().size()) continue;
            XWPFTableRow row = tbl.getRow(rm.rowIdx());
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 4) continue;

            XWPFTableCell cellSelf = cells.get(3);
            XWPFTableCell cellRev = cells.size() > 4 ? cells.get(4) : null;
            XWPFTableCell cellBgh = cells.size() > 5 ? cells.get(5) : null;
            XWPFTableCell cellNote = cells.size() > 6 ? cells.get(6) : null;

            switch (rm.type()) {
                case HEADER:
                    break;

                case SECTION_EMPTY:
                    // Dòng C, C.I, C.II: Không hiển thị số điểm
                    setCellText(cellSelf, "", false, ParagraphAlignment.CENTER);
                    if (cellRev != null) setCellText(cellRev, "", false, ParagraphAlignment.CENTER);
                    if (cellBgh != null) setCellText(cellBgh, "", false, ParagraphAlignment.CENTER);
                    break;

                case SUBTOTAL:
                    Double selfSubtotal = getSubtotalValue(rm.key(), form, eval);
                    if (selfSubtotal != null && (selfSubtotal > 0 || hasSelfScoredSection(rm.key(), form))) {
                        setCellText(cellSelf, formatScore(selfSubtotal), true, ParagraphAlignment.CENTER);
                    } else {
                        setCellText(cellSelf, "", false, ParagraphAlignment.CENTER);
                    }
                    if (cellRev != null) {
                        if (hasReviewer) {
                            Double revSubtotal = getReviewerSubtotalValue(rm.key(), form, eval);
                            if (revSubtotal != null && (revSubtotal > 0 || hasReviewerScoredSection(rm.key(), form))) {
                                setCellText(cellRev, formatScore(revSubtotal), true, ParagraphAlignment.CENTER);
                            } else {
                                setCellText(cellRev, "", false, ParagraphAlignment.CENTER);
                            }
                        } else {
                            setCellText(cellRev, "-", false, ParagraphAlignment.CENTER);
                        }
                    }
                    if (cellBgh != null) {
                        if (hasBgh) {
                            Double bghSubtotal = getBghSubtotalValue(rm.key(), form, eval);
                            if (bghSubtotal != null && (bghSubtotal > 0 || hasBghScoredSection(rm.key(), form))) {
                                setCellText(cellBgh, formatScore(bghSubtotal), true, ParagraphAlignment.CENTER);
                            } else {
                                setCellText(cellBgh, "", false, ParagraphAlignment.CENTER);
                            }
                        } else {
                            setCellText(cellBgh, "-", false, ParagraphAlignment.CENTER);
                        }
                    }
                    break;

                case CRITERIA:
                    String k = rm.key();
                    Double sVal = null;
                    boolean isSelectedSelfB = false;

                    // Xử lý nhóm chọn radio B1, B2, B3
                    if (k.startsWith("B_1_")) {
                        isSelectedSelfB = isOptionMatch(k, "B_1_", form.getSelectedB1());
                        if (isSelectedSelfB) sVal = getBScore(k);
                    } else if (k.startsWith("B_2_")) {
                        isSelectedSelfB = isOptionMatch(k, "B_2_", form.getSelectedB2());
                        if (isSelectedSelfB) sVal = getBScore(k);
                    } else if (k.startsWith("B_3_")) {
                        isSelectedSelfB = isOptionMatch(k, "B_3_", form.getSelectedB3());
                        if (isSelectedSelfB) sVal = getBScore(k);
                    } else if (selfScores != null && selfScores.containsKey(k)) {
                        sVal = selfScores.get(k);
                    }

                    if (sVal != null && (sVal > 0 || (selfScores != null && selfScores.containsKey(k)) || isSelectedSelfB)) {
                        setCellText(cellSelf, formatScore(sVal), false, ParagraphAlignment.CENTER);
                    } else {
                        setCellText(cellSelf, "", false, ParagraphAlignment.CENTER);
                    }

                    // Điểm Tổ trưởng
                    if (cellRev != null) {
                        Double rVal = null;
                        boolean isSelectedRevB = false;
                        if (hasReviewer) {
                            if (k.startsWith("B_1_")) {
                                isSelectedRevB = isOptionMatch(k, "B_1_", form.getReviewerSelectedB1());
                                if (isSelectedRevB) rVal = getBScore(k);
                            } else if (k.startsWith("B_2_")) {
                                isSelectedRevB = isOptionMatch(k, "B_2_", form.getReviewerSelectedB2());
                                if (isSelectedRevB) rVal = getBScore(k);
                            } else if (k.startsWith("B_3_")) {
                                isSelectedRevB = isOptionMatch(k, "B_3_", form.getReviewerSelectedB3());
                                if (isSelectedRevB) rVal = getBScore(k);
                            } else if (revScores != null && revScores.containsKey(k)) {
                                rVal = revScores.get(k);
                            }
                        }

                        if (rVal != null && (rVal > 0 || (revScores != null && revScores.containsKey(k)) || isSelectedRevB)) {
                            setCellText(cellRev, formatScore(rVal), false, ParagraphAlignment.CENTER);
                        } else {
                            setCellText(cellRev, hasReviewer ? "" : "-", false, ParagraphAlignment.CENTER);
                        }
                    }

                    // Điểm Hội đồng thi đua, khen thưởng (Ban Giám Hiệu)
                    if (cellBgh != null) {
                        Double bVal = null;
                        boolean isSelectedBghB = false;
                        if (hasBgh) {
                            if (k.startsWith("B_1_")) {
                                isSelectedBghB = isOptionMatch(k, "B_1_", form.getBghSelectedB1());
                                if (isSelectedBghB) bVal = getBScore(k);
                            } else if (k.startsWith("B_2_")) {
                                isSelectedBghB = isOptionMatch(k, "B_2_", form.getBghSelectedB2());
                                if (isSelectedBghB) bVal = getBScore(k);
                            } else if (k.startsWith("B_3_")) {
                                isSelectedBghB = isOptionMatch(k, "B_3_", form.getBghSelectedB3());
                                if (isSelectedBghB) bVal = getBScore(k);
                            } else if (bghScores != null && bghScores.containsKey(k)) {
                                bVal = bghScores.get(k);
                            }
                        }

                        if (bVal != null && (bVal > 0 || (bghScores != null && bghScores.containsKey(k)) || isSelectedBghB)) {
                            setCellText(cellBgh, formatScore(bVal), false, ParagraphAlignment.CENTER);
                        } else {
                            setCellText(cellBgh, hasBgh ? "" : "-", false, ParagraphAlignment.CENTER);
                        }
                    }

                    // Giải trình
                    if (cellNote != null && explanations != null && explanations.containsKey(k)) {
                        String exp = explanations.get(k);
                        if (exp != null && !exp.isBlank()) {
                            setCellText(cellNote, exp, false, ParagraphAlignment.LEFT);
                        }
                    }
                    break;
            }
        }
    }

    private boolean hasSelfScoredSection(String key, Nq52EvaluationForm form) {
        if (form == null || form.getSelfScores() == null) return false;
        Map<String, Double> s = form.getSelfScores();
        return switch (key) {
            case "A" -> hasSelfScoredSection("A_I", form) || hasSelfScoredSection("A_II", form) || hasSelfScoredSection("A_III", form);
            case "A_I" -> hasSelfScoredSection("A_I_1", form) || hasSelfScoredSection("A_I_2", form);
            case "A_I_1" -> s.containsKey("A_I_1_a") || s.containsKey("A_I_1_b") || s.containsKey("A_I_1_c") || s.containsKey("A_I_1_d") || s.containsKey("A_I_1_dd") || s.containsKey("A_I_1_e") || s.containsKey("A_I_1_ee") || s.containsKey("A_I_1_g");
            case "A_I_2" -> s.containsKey("A_I_2_a") || s.containsKey("A_I_2_b") || s.containsKey("A_I_2_c") || s.containsKey("A_I_2_d");
            case "A_II" -> hasSelfScoredSection("A_II_1", form) || hasSelfScoredSection("A_II_2", form) || hasSelfScoredSection("A_II_3", form) || hasSelfScoredSection("A_II_4", form);
            case "A_II_1" -> s.containsKey("A_II_1_a") || s.containsKey("A_II_1_b") || s.containsKey("A_II_1_c");
            case "A_II_2" -> s.containsKey("A_II_2_a") || s.containsKey("A_II_2_b");
            case "A_II_3" -> s.containsKey("A_II_3_a") || s.containsKey("A_II_3_b");
            case "A_II_4" -> s.containsKey("A_II_4_a") || s.containsKey("A_II_4_b");
            case "A_III" -> s.containsKey("A_III_1") || s.containsKey("A_III_2") || s.containsKey("A_III_3") || s.containsKey("A_III_4");
            case "B" -> form.getSelectedB1() != null || form.getSelectedB2() != null || form.getSelectedB3() != null;
            case "B_1" -> form.getSelectedB1() != null;
            case "B_2" -> form.getSelectedB2() != null;
            case "B_3" -> form.getSelectedB3() != null;
            case "A_PLUS_B" -> hasSelfScoredSection("A", form) || hasSelfScoredSection("B", form);
            default -> false;
        };
    }

    private boolean hasReviewerScoredSection(String key, Nq52EvaluationForm form) {
        if (form == null || form.getReviewerScores() == null) return false;
        Map<String, Double> r = form.getReviewerScores();
        return switch (key) {
            case "A" -> hasReviewerScoredSection("A_I", form) || hasReviewerScoredSection("A_II", form) || hasReviewerScoredSection("A_III", form);
            case "A_I" -> hasReviewerScoredSection("A_I_1", form) || hasReviewerScoredSection("A_I_2", form);
            case "A_I_1" -> r.containsKey("A_I_1_a") || r.containsKey("A_I_1_b") || r.containsKey("A_I_1_c") || r.containsKey("A_I_1_d") || r.containsKey("A_I_1_dd") || r.containsKey("A_I_1_e") || r.containsKey("A_I_1_ee") || r.containsKey("A_I_1_g");
            case "A_I_2" -> r.containsKey("A_I_2_a") || r.containsKey("A_I_2_b") || r.containsKey("A_I_2_c") || r.containsKey("A_I_2_d");
            case "A_II" -> hasReviewerScoredSection("A_II_1", form) || hasReviewerScoredSection("A_II_2", form) || hasReviewerScoredSection("A_II_3", form) || hasReviewerScoredSection("A_II_4", form);
            case "A_II_1" -> r.containsKey("A_II_1_a") || r.containsKey("A_II_1_b") || r.containsKey("A_II_1_c");
            case "A_II_2" -> r.containsKey("A_II_2_a") || r.containsKey("A_II_2_b");
            case "A_II_3" -> r.containsKey("A_II_3_a") || r.containsKey("A_II_3_b");
            case "A_II_4" -> r.containsKey("A_II_4_a") || r.containsKey("A_II_4_b");
            case "A_III" -> r.containsKey("A_III_1") || r.containsKey("A_III_2") || r.containsKey("A_III_3") || r.containsKey("A_III_4");
            case "B" -> form.getReviewerSelectedB1() != null || form.getReviewerSelectedB2() != null || form.getReviewerSelectedB3() != null;
            case "B_1" -> form.getReviewerSelectedB1() != null;
            case "B_2" -> form.getReviewerSelectedB2() != null;
            case "B_3" -> form.getReviewerSelectedB3() != null;
            case "A_PLUS_B" -> hasReviewerScoredSection("A", form) || hasReviewerScoredSection("B", form);
            default -> false;
        };
    }

    private boolean isOptionMatch(String rowKey, String prefix, String selected) {
        if (selected == null || rowKey == null) return false;
        if (rowKey.equals(selected)) return true;
        if (rowKey.equals(prefix + selected)) return true;
        String normSelected = selected.startsWith(prefix) ? selected.substring(prefix.length()) : selected;
        String normRowKey = rowKey.startsWith(prefix) ? rowKey.substring(prefix.length()) : rowKey;
        return normRowKey.equals(normSelected);
    }

    private void fillClassificationCheckbox(XWPFDocument doc, Nq52Evaluation eval) {
        if (doc.getTables().size() < 6) return;
        XWPFTable t5 = doc.getTables().get(5);
        if (t5.getRows().isEmpty()) return;
        XWPFTableRow r = t5.getRow(0);
        List<XWPFTableCell> cells = r.getTableCells();
        if (cells.size() < 7) return;

        // Xóa dấu X cũ trong p[0] của 4 ô xếp loại (Cell 0, 2, 4, 6) và các ô đệm (Cell 1, 3, 5)
        for (int cIdx : new int[]{0, 2, 4, 6}) {
            if (cIdx < cells.size()) {
                clearParagraphRuns(cells.get(cIdx), 0);
            }
        }
        for (int spacerIdx : new int[]{1, 3, 5}) {
            if (spacerIdx < cells.size()) {
                clearCell(cells.get(spacerIdx));
            }
        }

        String selfClass = eval.getBghClassification() != null && !eval.getBghClassification().isBlank()
                ? eval.getBghClassification()
                : (eval.getReviewerClassification() != null && !eval.getReviewerClassification().isBlank()
                    ? eval.getReviewerClassification()
                    : eval.getSelfClassification());
        if (selfClass == null || selfClass.isBlank()) return;
        selfClass = selfClass.trim().toLowerCase();

        // Cell 0: Hoàn thành xuất sắc nhiệm vụ (Từ 90 điểm trở lên)
        // Cell 2: Hoàn thành tốt nhiệm vụ (Từ 70 điểm đến dưới 90 điểm)
        // Cell 4: Hoàn thành nhiệm vụ (Từ 50 điểm đến dưới 70 điểm)
        // Cell 6: Không hoàn thành nhiệm vụ (Dưới 50 điểm)
        // Lưu ý: Cell 1, 3, 5 là các cột khoảng cách (spacer) giữa các khung
        int targetCellIdx = -1;
        if (selfClass.contains("xuất sắc")) {
            targetCellIdx = 0;
        } else if (selfClass.contains("không hoàn thành")) {
            targetCellIdx = 6;
        } else if (selfClass.contains("tốt")) {
            targetCellIdx = 2;
        } else if (selfClass.contains("hoàn thành")) {
            targetCellIdx = 4;
        }

        if (targetCellIdx >= 0 && targetCellIdx < cells.size()) {
            XWPFTableCell cell = cells.get(targetCellIdx);
            if (!cell.getParagraphs().isEmpty()) {
                XWPFParagraph p0 = cell.getParagraphs().get(0);
                while (!p0.getRuns().isEmpty()) {
                    p0.removeRun(0);
                }
                p0.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun run = p0.createRun();
                run.setText("X");
                run.setFontFamily("Times New Roman");
                run.setFontSize(13);
                run.setBold(true);
            }
        }
    }

    private void clearParagraphRuns(XWPFTableCell cell, int paraIdx) {
        if (cell != null && cell.getParagraphs().size() > paraIdx) {
            XWPFParagraph p = cell.getParagraphs().get(paraIdx);
            while (!p.getRuns().isEmpty()) {
                p.removeRun(0);
            }
        }
    }

    private void clearCell(XWPFTableCell cell) {
        if (cell != null) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                while (!p.getRuns().isEmpty()) {
                    p.removeRun(0);
                }
            }
        }
    }

    private Double getSubtotalValue(String key, Nq52EvaluationForm form, Nq52Evaluation eval) {
        return switch (key) {
            case "A" -> Double.valueOf(form.getSelfScoreA());
            case "A_I" -> Double.valueOf(form.getSelfScoreAi());
            case "A_I_1" -> Double.valueOf(form.getSelfScoreAi1());
            case "A_I_2" -> Double.valueOf(form.getSelfScoreAi2());
            case "A_II" -> Double.valueOf(form.getSelfScoreAii());
            case "A_II_1" -> Double.valueOf(form.getSelfScoreAii1());
            case "A_II_2" -> Double.valueOf(form.getSelfScoreAii2());
            case "A_II_3" -> Double.valueOf(form.getSelfScoreAii3());
            case "A_II_4" -> Double.valueOf(form.getSelfScoreAii4());
            case "A_III" -> Double.valueOf(form.getSelfScoreAiii());
            case "B" -> Double.valueOf(form.getSelfScoreB());
            case "B_1" -> Double.valueOf(form.getSelfScoreB1());
            case "B_2" -> Double.valueOf(form.getSelfScoreB2());
            case "B_3" -> Double.valueOf(form.getSelfScoreB3());
            case "A_PLUS_B" -> Double.valueOf(form.getSelfScoreAb());
            default -> null;
        };
    }

    private Double getReviewerSubtotalValue(String key, Nq52EvaluationForm form, Nq52Evaluation eval) {
        if (!form.hasReviewerData() && eval.getStatus() != Nq52Status.REVIEWED) {
            return null;
        }
        return switch (key) {
            case "A" -> form.getReviewerScoreA();
            case "A_I" -> form.getReviewerScoreAi();
            case "A_I_1" -> form.getReviewerScoreAi1();
            case "A_I_2" -> form.getReviewerScoreAi2();
            case "A_II" -> form.getReviewerScoreAii();
            case "A_II_1" -> form.getReviewerScoreAii1();
            case "A_II_2" -> form.getReviewerScoreAii2();
            case "A_II_3" -> form.getReviewerScoreAii3();
            case "A_II_4" -> form.getReviewerScoreAii4();
            case "A_III" -> form.getReviewerScoreAiii();
            case "B" -> form.getReviewerScoreB();
            case "B_1" -> form.getReviewerScoreB1();
            case "B_2" -> form.getReviewerScoreB2();
            case "B_3" -> form.getReviewerScoreB3();
            case "A_PLUS_B" -> form.getReviewerScoreAb();
            default -> null;
        };
    }

    private Double getBghSubtotalValue(String key, Nq52EvaluationForm form, Nq52Evaluation eval) {
        if (!form.hasBghData() && eval.getStatus() != Nq52Status.COUNCIL_REVIEWED && eval.getBghTotalScore() == null) {
            return null;
        }
        return switch (key) {
            case "A" -> form.getBghScoreA();
            case "A_I" -> form.getBghScoreAi();
            case "A_I_1" -> form.getBghScoreAi1();
            case "A_I_2" -> form.getBghScoreAi2();
            case "A_II" -> form.getBghScoreAii();
            case "A_II_1" -> form.getBghScoreAii1();
            case "A_II_2" -> form.getBghScoreAii2();
            case "A_II_3" -> form.getBghScoreAii3();
            case "A_II_4" -> form.getBghScoreAii4();
            case "A_III" -> form.getBghScoreAiii();
            case "B" -> form.getBghScoreB();
            case "B_1" -> form.getBghScoreB1();
            case "B_2" -> form.getBghScoreB2();
            case "B_3" -> form.getBghScoreB3();
            case "A_PLUS_B" -> form.getBghScoreAb();
            default -> null;
        };
    }

    private boolean hasBghScoredSection(String key, Nq52EvaluationForm form) {
        if (form == null || form.getBghScores() == null) return false;
        Map<String, Double> b = form.getBghScores();
        return switch (key) {
            case "A" -> hasBghScoredSection("A_I", form) || hasBghScoredSection("A_II", form) || hasBghScoredSection("A_III", form);
            case "A_I" -> hasBghScoredSection("A_I_1", form) || hasBghScoredSection("A_I_2", form);
            case "A_I_1" -> b.containsKey("A_I_1_a") || b.containsKey("A_I_1_b") || b.containsKey("A_I_1_c") || b.containsKey("A_I_1_d") || b.containsKey("A_I_1_dd") || b.containsKey("A_I_1_e") || b.containsKey("A_I_1_ee") || b.containsKey("A_I_1_g");
            case "A_I_2" -> b.containsKey("A_I_2_a") || b.containsKey("A_I_2_b") || b.containsKey("A_I_2_c") || b.containsKey("A_I_2_d");
            case "A_II" -> hasBghScoredSection("A_II_1", form) || hasBghScoredSection("A_II_2", form) || hasBghScoredSection("A_II_3", form) || hasBghScoredSection("A_II_4", form);
            case "A_II_1" -> b.containsKey("A_II_1_a") || b.containsKey("A_II_1_b") || b.containsKey("A_II_1_c");
            case "A_II_2" -> b.containsKey("A_II_2_a") || b.containsKey("A_II_2_b");
            case "A_II_3" -> b.containsKey("A_II_3_a") || b.containsKey("A_II_3_b");
            case "A_II_4" -> b.containsKey("A_II_4_a") || b.containsKey("A_II_4_b");
            case "A_III" -> b.containsKey("A_III_1") || b.containsKey("A_III_2") || b.containsKey("A_III_3") || b.containsKey("A_III_4");
            case "B" -> form.getBghSelectedB1() != null || form.getBghSelectedB2() != null || form.getBghSelectedB3() != null;
            case "B_1" -> form.getBghSelectedB1() != null;
            case "B_2" -> form.getBghSelectedB2() != null;
            case "B_3" -> form.getBghSelectedB3() != null;
            case "A_PLUS_B" -> hasBghScoredSection("A", form) || hasBghScoredSection("B", form);
            default -> false;
        };
    }

    private Double getBScore(String key) {
        return switch (key) {
            case "B_1_a", "B_2_a" -> 20.0;
            case "B_1_b", "B_2_b" -> 15.0;
            case "B_1_c", "B_2_c" -> 10.0;
            case "B_1_d", "B_2_d" -> 5.0;
            case "B_1_dd", "B_2_dd" -> 1.0;
            case "B_1_e", "B_2_e", "B_3_g" -> 0.0;
            case "B_3_a" -> 30.0;
            case "B_3_b" -> 25.0;
            case "B_3_c" -> 20.0;
            case "B_3_d" -> 15.0;
            case "B_3_dd" -> 10.0;
            case "B_3_e" -> 5.0;
            default -> 0.0;
        };
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, ParagraphAlignment alignment) {
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        while (!p.getRuns().isEmpty()) {
            p.removeRun(0);
        }
        p.setAlignment(alignment);
        if (text != null && !text.isEmpty()) {
            XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontFamily("Times New Roman");
            run.setFontSize(11);
            run.setBold(bold);
        }
    }

    private void replaceParagraphText(XWPFParagraph p, String newText, boolean bold, boolean italic, ParagraphAlignment alignment) {
        while (!p.getRuns().isEmpty()) {
            p.removeRun(0);
        }
        if (alignment != null) {
            p.setAlignment(alignment);
        }
        XWPFRun run = p.createRun();
        run.setText(newText);
        run.setFontFamily("Times New Roman");
        run.setFontSize(12);
        run.setBold(bold);
        run.setItalic(italic);
    }

    private void insertTeacherSignatureName(XWPFDocument doc, int afterParaIdx, String teacherName) {
        if (afterParaIdx + 5 < doc.getParagraphs().size()) {
            XWPFParagraph p = doc.getParagraphs().get(afterParaIdx + 5);
            if (p.getText().isBlank()) {
                replaceParagraphText(p, teacherName, true, false, ParagraphAlignment.CENTER);
                p.setIndentationLeft(3850);
                p.setIndentationRight(0);
                p.setIndentationFirstLine(0);
                p.setIndentationHanging(0);
            }
        }
    }

    private void insertReviewerSignatureName(XWPFDocument doc, int afterParaIdx, String reviewerName) {
        if (afterParaIdx + 6 < doc.getParagraphs().size()) {
            XWPFParagraph p = doc.getParagraphs().get(afterParaIdx + 6);
            if (p.getText().isBlank()) {
                replaceParagraphText(p, reviewerName, true, false, ParagraphAlignment.CENTER);
                p.setIndentationLeft(5137);
                p.setIndentationRight(469);
                p.setIndentationFirstLine(0);
                p.setIndentationHanging(0);
            }
        }
    }

    private String formatScore(Double score) {
        if (score == null) return "";
        if (Math.abs(score - Math.round(score)) < 0.001) {
            return String.valueOf(Math.round(score));
        }
        return SCORE_FMT.format(score);
    }

    private String formatScore(BigDecimal score) {
        if (score == null) return "0";
        BigDecimal rounded = score.setScale(2, RoundingMode.HALF_UP);
        if (rounded.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return String.valueOf(rounded.intValue());
        }
        return SCORE_FMT.format(rounded);
    }
}
