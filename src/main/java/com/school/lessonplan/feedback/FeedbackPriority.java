package com.school.lessonplan.feedback;

public enum FeedbackPriority {
    LOW("Thấp (Tham khảo)"),
    NORMAL("Bình thường"),
    HIGH("Cao (Cần ưu tiên)"),
    URGENT("Khẩn cấp (Cản trở giảng dạy)");

    private final String displayName;

    FeedbackPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
