package com.school.lessonplan.nq52;

public enum Nq52Status {
    DRAFT("Bản nháp", "badge-draft"),
    SUBMITTED("Đã nộp Tổ trưởng", "badge-submitted"),
    REVIEWED("Tổ trưởng đã đánh giá", "badge-reviewed");

    private final String displayName;
    private final String cssClass;

    Nq52Status(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }
}

