package com.school.lessonplan.nq52.signature.dto;

public class UserSignatureProfileForm {

    private String moetId;
    private String fullName;
    private String dob;
    private String gender;
    private String nationalId;
    private String personalId;

    private String signatureType;
    private String caProvider;
    private String caUsername;
    private String caPassword;

    private String certSerial;
    private String certSubject;
    private String validFrom;
    private String validTo;
    private String ownerInfo;

    private Boolean autoAttachSignature;
    private String signatureImageBase64;

    public UserSignatureProfileForm() {}

    public String getMoetId() { return moetId; }
    public void setMoetId(String moetId) { this.moetId = moetId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getPersonalId() { return personalId; }
    public void setPersonalId(String personalId) { this.personalId = personalId; }

    public String getSignatureType() { return signatureType; }
    public void setSignatureType(String signatureType) { this.signatureType = signatureType; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }

    public String getCaUsername() { return caUsername; }
    public void setCaUsername(String caUsername) { this.caUsername = caUsername; }

    public String getCaPassword() { return caPassword; }
    public void setCaPassword(String caPassword) { this.caPassword = caPassword; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }

    public String getValidTo() { return validTo; }
    public void setValidTo(String validTo) { this.validTo = validTo; }

    public String getOwnerInfo() { return ownerInfo; }
    public void setOwnerInfo(String ownerInfo) { this.ownerInfo = ownerInfo; }

    public Boolean getAutoAttachSignature() { return autoAttachSignature; }
    public void setAutoAttachSignature(Boolean autoAttachSignature) { this.autoAttachSignature = autoAttachSignature; }

    public String getSignatureImageBase64() { return signatureImageBase64; }
    public void setSignatureImageBase64(String signatureImageBase64) { this.signatureImageBase64 = signatureImageBase64; }
}

