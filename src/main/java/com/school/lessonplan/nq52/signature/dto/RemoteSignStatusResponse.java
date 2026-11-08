package com.school.lessonplan.nq52.signature.dto;

public class RemoteSignStatusResponse {
    private String transactionId;
    private String status; // 'WAITING', 'APPROVED', 'REJECTED', 'EXPIRED', 'COMPLETED'
    private String message;
    private boolean completed;
    private String signedAtFormatted;
    private String signerName;
    private String caProvider;

    public RemoteSignStatusResponse() {}

    public RemoteSignStatusResponse(String transactionId, String status, String message, boolean completed) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.completed = completed;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getSignedAtFormatted() { return signedAtFormatted; }
    public void setSignedAtFormatted(String signedAtFormatted) { this.signedAtFormatted = signedAtFormatted; }

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }
}

