// src/main/java/com/school/lessonplan/ai/dto/ChecklistLevel.java
package com.school.lessonplan.ai.dto;

public enum ChecklistLevel {
    LOI_NGHIEM_TRONG,
    CAN_BO_SUNG,
    GOI_Y;

    public static ChecklistLevel fromString(String val) {
        if (val == null || val.isBlank()) {
            return GOI_Y;
        }
        try {
            return ChecklistLevel.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GOI_Y;
        }
    }
}
