package com.school.lessonplan.feedback;

public enum FeedbackStatus {
    PENDING("Chờ tiếp nhận", "badge--warning"),
    IN_PROGRESS("Đang xử lý", "badge--info"),
    RESOLVED("Đã giải quyết", "badge--success"),
    CLOSED("Đã đóng", "badge--secondary");

    private final String displayName;
    private final String badgeClass;

    FeedbackStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
