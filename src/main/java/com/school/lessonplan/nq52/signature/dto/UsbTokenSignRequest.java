package com.school.lessonplan.nq52.signature.dto;

public class UsbTokenSignRequest {
    private String documentType;
    private Long documentId;
    private String certSerial;
    private String certSubject;
    private String certIssuer;
    private String certValidFrom;
    private String certValidTo;
    private String caProvider;
    private String pin;
    private String bghNotes;
    private String dataHashSha256;
    private String signatureValue;
    private String verificationDetails;

    public UsbTokenSignRequest() {}

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getCertIssuer() { return certIssuer; }
    public void setCertIssuer(String certIssuer) { this.certIssuer = certIssuer; }

    public String getCertValidFrom() { return certValidFrom; }
    public void setCertValidFrom(String certValidFrom) { this.certValidFrom = certValidFrom; }

    public String getCertValidTo() { return certValidTo; }
    public void setCertValidTo(String certValidTo) { this.certValidTo = certValidTo; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getBghNotes() { return bghNotes; }
    public void setBghNotes(String bghNotes) { this.bghNotes = bghNotes; }

    public String getDataHashSha256() { return dataHashSha256; }
    public void setDataHashSha256(String dataHashSha256) { this.dataHashSha256 = dataHashSha256; }

    public String getSignatureValue() { return signatureValue; }
    public void setSignatureValue(String signatureValue) { this.signatureValue = signatureValue; }

    public String getVerificationDetails() { return verificationDetails; }
    public void setVerificationDetails(String verificationDetails) { this.verificationDetails = verificationDetails; }
}

