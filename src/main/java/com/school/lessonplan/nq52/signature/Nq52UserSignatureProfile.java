package com.school.lessonplan.nq52.signature;

import com.school.lessonplan.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "nq52_user_signature_profiles")
public class Nq52UserSignatureProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // === Thông tin nhân sự ===
    @Column(name = "moet_id", length = 50)
    private String moetId;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "dob", length = 50)
    private String dob;

    @Column(name = "gender", length = 20)
    private String gender = "Nam";

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "personal_id", length = 50)
    private String personalId;

    // === Thông tin đăng ký tài khoản chữ ký số ===
    @Column(name = "registration_status", length = 100)
    private String registrationStatus = "Đã duyệt tài khoản";

    @Column(name = "signature_type", length = 100)
    private String signatureType = "REMOTE SIGNING";

    @Column(name = "ca_provider", length = 255)
    private String caProvider = "Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)";

    @Column(name = "ca_username", length = 255)
    private String caUsername;

    // === Thông tin đăng ký chứng thư số ===
    @Column(name = "cert_serial", length = 100)
    private String certSerial;

    @Column(name = "cert_subject", length = 500)
    private String certSubject;

    @Column(name = "valid_from", length = 50)
    private String validFrom;

    @Column(name = "valid_to", length = 50)
    private String validTo;

    @Column(name = "owner_info", length = 500)
    private String ownerInfo;

    // === Thông tin hình ảnh chữ ký ===
    @Column(name = "signature_image_base64", columnDefinition = "NVARCHAR(MAX)")
    private String signatureImageBase64;

    @Column(name = "auto_attach_signature")
    private Boolean autoAttachSignature = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Nq52UserSignatureProfile() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (autoAttachSignature == null) autoAttachSignature = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // === Getters & Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public String getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(String registrationStatus) { this.registrationStatus = registrationStatus; }

    public String getSignatureType() { return signatureType; }
    public void setSignatureType(String signatureType) { this.signatureType = signatureType; }

    public String getCaProvider() { return caProvider; }
    public void setCaProvider(String caProvider) { this.caProvider = caProvider; }

    public String getCaUsername() { return caUsername; }
    public void setCaUsername(String caUsername) { this.caUsername = caUsername; }

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

    public String getSignatureImageBase64() { return signatureImageBase64; }
    public void setSignatureImageBase64(String signatureImageBase64) { this.signatureImageBase64 = signatureImageBase64; }

    public Boolean getAutoAttachSignature() { return autoAttachSignature; }
    public void setAutoAttachSignature(Boolean autoAttachSignature) { this.autoAttachSignature = autoAttachSignature; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

