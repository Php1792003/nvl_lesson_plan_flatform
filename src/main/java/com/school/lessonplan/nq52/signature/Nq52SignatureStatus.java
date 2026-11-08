package com.school.lessonplan.nq52.signature;

public enum Nq52SignatureStatus {
    VALID("Hợp lệ"),
    REVOKED("Đã thu hồi");

    private final String displayName;

    Nq52SignatureStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

