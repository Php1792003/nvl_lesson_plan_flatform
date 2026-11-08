package com.school.lessonplan.nq52.signature.dto;

public class SignatureItemDto {
    private Long id;
    private String signingRole;
    private String signingRoleDisplayName;
    private String signerName;
    private String signerPosition;
    private String workUnit;
    private String signatureType;
    private String signatureTypeDisplayName;
    private String caProvider;
    private String certSubject;
    private String certSerial;
    private String certIssuer;
    private String signedAtFormatted;
    private String dataHashSha256;
    private boolean valid;
    private String signatureImageBase64;

    public SignatureItemDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSigningRole() { return signingRole; }
    public void setSigningRole(String signingRole) { this.signingRole = signingRole; }

    public String getSigningRoleDisplayName() { return signingRoleDisplayName; }
    public void setSigningRoleDisplayName(String signingRoleDisplayName) { this.signingRoleDisplayName = signingRoleDisplayName; }

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }

    public String getSignerPosition() { return signerPosition; }
    public void setSignerPosition(String signerPosition) { this.signerPosition = signerPosition; }

    public String getWorkUnit() { return workUnit; }
    public void setWorkUnit(String workUnit) { this.workUnit = workUnit; }

    public String getSignatureType() { return signatureType; }
    public void setSignatureType(String signatureType) { this.signatureType = signatureType; }

    public String getSignatureTypeDisplayName() { return signatureTypeDisplayName; }
    public void setSignatureTypeDisplayName(String signatureTypeDisplayName) { this.signatureTypeDisplayName = signatureTypeDisplayName; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertIssuer() { return certIssuer; }
    public void setCertIssuer(String certIssuer) { this.certIssuer = certIssuer; }

    public String getSignedAtFormatted() { return signedAtFormatted; }
    public void setSignedAtFormatted(String signedAtFormatted) { this.signedAtFormatted = signedAtFormatted; }

    public String getDataHashSha256() { return dataHashSha256; }
    public void setDataHashSha256(String dataHashSha256) { this.dataHashSha256 = dataHashSha256; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getSignatureImageBase64() { return signatureImageBase64; }
    public void setSignatureImageBase64(String signatureImageBase64) { this.signatureImageBase64 = signatureImageBase64; }
}

