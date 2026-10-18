// src/main/java/com/school/lessonplan/ai/dto/InputType.java
package com.school.lessonplan.ai.dto;

public enum InputType {
    FILE_TRONG,
    SAI_LOAI_TAI_LIEU,
    GIAO_AN_KHONG_DAY_DU,
    GIAO_AN_HOP_LE;

    public static InputType fromString(String val) {
        if (val == null || val.isBlank()) {
            return GIAO_AN_KHONG_DAY_DU;
        }
        try {
            return InputType.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GIAO_AN_KHONG_DAY_DU;
        }
    }
}
