// src/main/java/com/school/lessonplan/ai/dto/ReviewVerdict.java
package com.school.lessonplan.ai.dto;

public enum ReviewVerdict {
    DUYET,
    DUYET_CO_DIEU_KIEN,
    KHONG_DUYET;

    public static ReviewVerdict fromString(String val) {
        if (val == null || val.isBlank()) {
            return KHONG_DUYET;
        }
        try {
            return ReviewVerdict.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return KHONG_DUYET;
        }
    }
}
