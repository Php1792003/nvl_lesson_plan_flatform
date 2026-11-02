package com.school.lessonplan.nq52.signature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Nq52DigitalSignatureRepository extends JpaRepository<Nq52DigitalSignature, Long> {

    List<Nq52DigitalSignature> findByDocumentTypeAndDocumentIdOrderBySignedAtAsc(String documentType, Long documentId);

    List<Nq52DigitalSignature> findByDocumentTypeAndDocumentIdAndStatusOrderBySignedAtAsc(
            String documentType, Long documentId, Nq52SignatureStatus status);

    Optional<Nq52DigitalSignature> findFirstByDocumentTypeAndDocumentIdAndSigningRoleAndStatusOrderBySignedAtDesc(
            String documentType, Long documentId, Nq52SigningRole signingRole, Nq52SignatureStatus status);

    @Query("SELECT s FROM Nq52DigitalSignature s WHERE s.documentType = :docType AND s.documentId = :docId AND s.status = 'VALID'")
    List<Nq52DigitalSignature> findActiveSignatures(@Param("docType") String docType, @Param("docId") Long docId);
}

