package com.school.lessonplan.nq52.signature.dto;

import java.util.List;

public class RealtimeVerificationResultDto {
    private boolean valid;
    private boolean integrityMatched;
    private String ocspStatus;       // "GOOD", "REVOKED", "UNKNOWN"
    private String crlStatus;        // "NOT_REVOKED", "REVOKED"
    private String currentHash;
    private String signedHash;
    private String signerName;
    private String signerRoleDisplayName;
    private String caProvider;
    private String certSerial;
    private String certSubject;
    private String certIssuer;
    private String signedAtFormatted;
    private String verifiedAtFormatted;
    private String message;
    private List<SignatureItemDto> allSignatures;

    public RealtimeVerificationResultDto() {}

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public boolean isIntegrityMatched() { return integrityMatched; }
    public void setIntegrityMatched(boolean integrityMatched) { this.integrityMatched = integrityMatched; }

    public String getOcspStatus() { return ocspStatus; }
    public void setOcspStatus(String ocspStatus) { this.ocspStatus = ocspStatus; }

    public String getCrlStatus() { return crlStatus; }
    public void setCrlStatus(String crlStatus) { this.crlStatus = crlStatus; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public String getSignedHash() { return signedHash; }
    public void setSignedHash(String signedHash) { this.signedHash = signedHash; }

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }

    public String getSignerRoleDisplayName() { return signerRoleDisplayName; }
    public void setSignerRoleDisplayName(String signerRoleDisplayName) { this.signerRoleDisplayName = signerRoleDisplayName; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getCertIssuer() { return certIssuer; }
    public void setCertIssuer(String certIssuer) { this.certIssuer = certIssuer; }

    public String getSignedAtFormatted() { return signedAtFormatted; }
    public void setSignedAtFormatted(String signedAtFormatted) { this.signedAtFormatted = signedAtFormatted; }

    public String getVerifiedAtFormatted() { return verifiedAtFormatted; }
    public void setVerifiedAtFormatted(String verifiedAtFormatted) { this.verifiedAtFormatted = verifiedAtFormatted; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<SignatureItemDto> getAllSignatures() { return allSignatures; }
    public void setAllSignatures(List<SignatureItemDto> allSignatures) { this.allSignatures = allSignatures; }
}

