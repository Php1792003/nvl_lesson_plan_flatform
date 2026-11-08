package com.school.lessonplan.nq52.signature;

public enum Nq52SignatureType {
    REMOTE_SIGNING("Ký số từ xa (Remote Signing)", "Cloud HSM / SmartCA"),
    USB_TOKEN("Ký số phần cứng (USB Token)", "PKCS#11 Hardware Token");

    private final String displayName;
    private final String description;

    Nq52SignatureType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

