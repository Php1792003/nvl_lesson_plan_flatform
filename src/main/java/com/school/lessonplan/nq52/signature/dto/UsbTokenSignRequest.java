package com.school.lessonplan.nq52.signature.dto;

public class UsbTokenSignRequest {
    private String documentType;
    private Long documentId;
    private String certSerial;
    private String certSubject;
    private String pin;
    private String bghNotes;

    public UsbTokenSignRequest() {}

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }
}

