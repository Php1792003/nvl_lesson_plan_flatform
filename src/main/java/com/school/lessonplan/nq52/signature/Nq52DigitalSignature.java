package com.school.lessonplan.nq52.signature;

import com.school.lessonplan.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "nq52_digital_signatures")
public class Nq52DigitalSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // 'WORK_RESULT' or 'EVALUATION'

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signing_role", nullable = false, length = 50)
    private Nq52SigningRole signingRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id", nullable = false)
    private User signer;

    @Column(name = "signer_name_snapshot", nullable = false, length = 255)
    private String signerNameSnapshot;

    @Column(name = "signer_position", nullable = false, length = 100)
    private String signerPosition;

    @Column(name = "work_unit", nullable = false, length = 300)
    private String workUnit = "Trường THCS Nguyễn Văn Linh";

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 50)
    private Nq52SignatureType signatureType;

    @Column(name = "ca_provider", nullable = false, length = 100)
    private String caProvider; // 'VNPT_SMARTCA', 'VIETTEL_CLOUD_CA', 'BAN_CO_YEU', 'VN_EID'...

    @Column(name = "cert_subject", length = 500)
    private String certSubject;

    @Column(name = "cert_serial", length = 100)
    private String certSerial;

    @Column(name = "cert_issuer", length = 500)
    private String certIssuer;

    @Column(name = "cert_valid_from")
    private Instant certValidFrom;

    @Column(name = "cert_valid_to")
    private Instant certValidTo;

    @Column(name = "data_hash_sha256", nullable = false, length = 128)
    private String dataHashSha256;

    @Column(name = "signature_value", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String signatureValue;

    @Column(name = "timestamp_token", columnDefinition = "NVARCHAR(MAX)")
    private String timestampToken;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Nq52SignatureStatus status = Nq52SignatureStatus.VALID;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "signature_image_base64", columnDefinition = "NVARCHAR(MAX)")
    private String signatureImageBase64;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Nq52DigitalSignature() {}

    @PrePersist
    protected void onCreate() {
        if (signedAt == null) signedAt = Instant.now();
        if (createdAt == null) createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Nq52SigningRole getSigningRole() { return signingRole; }
    public void setSigningRole(Nq52SigningRole signingRole) { this.signingRole = signingRole; }

    public User getSigner() { return signer; }
    public void setSigner(User signer) { this.signer = signer; }

    public String getSignerNameSnapshot() { return signerNameSnapshot; }
    public void setSignerNameSnapshot(String signerNameSnapshot) { this.signerNameSnapshot = signerNameSnapshot; }

    public String getSignerPosition() { return signerPosition; }
    public void setSignerPosition(String signerPosition) { this.signerPosition = signerPosition; }

    public String getWorkUnit() { return workUnit; }
    public void setWorkUnit(String workUnit) { this.workUnit = workUnit; }

    public Nq52SignatureType getSignatureType() { return signatureType; }
    public void setSignatureType(Nq52SignatureType signatureType) { this.signatureType = signatureType; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertIssuer() { return certIssuer; }
    public void setCertIssuer(String certIssuer) { this.certIssuer = certIssuer; }

    public Instant getCertValidFrom() { return certValidFrom; }
    public void setCertValidFrom(Instant certValidFrom) { this.certValidFrom = certValidFrom; }

    public Instant getCertValidTo() { return certValidTo; }
    public void setCertValidTo(Instant certValidTo) { this.certValidTo = certValidTo; }

    public String getDataHashSha256() { return dataHashSha256; }
    public void setDataHashSha256(String dataHashSha256) { this.dataHashSha256 = dataHashSha256; }

    public String getSignatureValue() { return signatureValue; }
    public void setSignatureValue(String signatureValue) { this.signatureValue = signatureValue; }

    public String getTimestampToken() { return timestampToken; }
    public void setTimestampToken(String timestampToken) { this.timestampToken = timestampToken; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Nq52SignatureStatus getStatus() { return status; }
    public void setStatus(Nq52SignatureStatus status) { this.status = status; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public String getSignatureImageBase64() { return signatureImageBase64; }
    public void setSignatureImageBase64(String signatureImageBase64) { this.signatureImageBase64 = signatureImageBase64; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
