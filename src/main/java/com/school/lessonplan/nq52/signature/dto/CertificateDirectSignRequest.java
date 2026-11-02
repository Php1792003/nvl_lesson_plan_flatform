package com.school.lessonplan.nq52.signature.dto;

public class CertificateDirectSignRequest {
    private String documentType; // "WORK_RESULT" or "EVALUATION"
    private Long documentId;
    private String provider = "VNPT_SMARTCA"; // "VNPT_SMARTCA", "VIETTEL_CLOUD_CA", "VN_EID"
    private String username;
    private String password;
    private String pin;
    private String reviewerNotes;

    public CertificateDirectSignRequest() {}

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }
}

