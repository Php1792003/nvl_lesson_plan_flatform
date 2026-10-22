package com.school.lessonplan.feedback;

public enum FeedbackCategory {
    CHUYEN_MON("Chuyên môn & Kế hoạch bài dạy"),
    HE_THONG("Báo lỗi hệ thống / Kỹ thuật"),
    TINH_NANG("Đề xuất tính năng mới"),
    CO_SO_VAT_CHAT("Cơ sở vật chất & Thiết bị dạy"),
    KHAC("Ý kiến đóng góp khác");

    private final String displayName;

    FeedbackCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
