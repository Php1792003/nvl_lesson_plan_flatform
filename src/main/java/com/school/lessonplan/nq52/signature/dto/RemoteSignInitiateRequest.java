package com.school.lessonplan.nq52.signature.dto;

public class RemoteSignInitiateRequest {
    private String documentType;
    private Long documentId;
    private String provider = "VNPT_SMARTCA"; // VNPT_SMARTCA, VIETTEL_CLOUD_CA, VN_EID
    private String reviewerNotes;

    public RemoteSignInitiateRequest() {}

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }
}

