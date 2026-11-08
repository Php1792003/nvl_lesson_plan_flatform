package com.school.lessonplan.nq52.signature;

public enum Nq52SigningRole {
    TEACHER("Giáo viên / Cá nhân tự đánh giá", "TEACHER"),
    DEPT_LEADER("Tổ trưởng chuyên môn", "DEPT_LEADER"),
    PRINCIPAL("Ban Giám Hiệu / Hiệu trưởng", "PRINCIPAL");

    private final String displayName;
    private final String code;

    Nq52SigningRole(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}

