package com.school.lessonplan.nq52.signature.dto;

import java.util.List;

public class PrepareSigningResponse {
    private String documentType;
    private Long documentId;
    private String documentTitle;
    private String dataHashSha256;
    private String signerName;
    private String signerRole;
    private String signerIdentifier;
    private String recommendedSignatureType; // 'REMOTE_SIGNING' or 'USB_TOKEN'
    private boolean hasConfiguredProfile;
    private String profileProvider;
    private String profileCertSerial;
    private String profileSubject;
    private String profileRegistrationStatus;
    private List<SignatureItemDto> existingSignatures;

    public PrepareSigningResponse() {}

    public boolean isHasConfiguredProfile() { return hasConfiguredProfile; }
    public void setHasConfiguredProfile(boolean hasConfiguredProfile) { this.hasConfiguredProfile = hasConfiguredProfile; }

    public String getProfileProvider() { return profileProvider; }
    public void setProfileProvider(String profileProvider) { this.profileProvider = profileProvider; }

    public String getProfileCertSerial() { return profileCertSerial; }
    public void setProfileCertSerial(String profileCertSerial) { this.profileCertSerial = profileCertSerial; }

    public String getProfileSubject() { return profileSubject; }
    public void setProfileSubject(String profileSubject) { this.profileSubject = profileSubject; }

    public String getProfileRegistrationStatus() { return profileRegistrationStatus; }
    public void setProfileRegistrationStatus(String profileRegistrationStatus) { this.profileRegistrationStatus = profileRegistrationStatus; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getDataHashSha256() { return dataHashSha256; }
    public void setDataHashSha256(String dataHashSha256) { this.dataHashSha256 = dataHashSha256; }

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }

    public String getSignerRole() { return signerRole; }
    public void setSignerRole(String signerRole) { this.signerRole = signerRole; }

    public String getSignerIdentifier() { return signerIdentifier; }
    public void setSignerIdentifier(String signerIdentifier) { this.signerIdentifier = signerIdentifier; }

    public String getRecommendedSignatureType() { return recommendedSignatureType; }
    public void setRecommendedSignatureType(String recommendedSignatureType) { this.recommendedSignatureType = recommendedSignatureType; }

    public List<SignatureItemDto> getExistingSignatures() { return existingSignatures; }
    public void setExistingSignatures(List<SignatureItemDto> existingSignatures) { this.existingSignatures = existingSignatures; }
}

