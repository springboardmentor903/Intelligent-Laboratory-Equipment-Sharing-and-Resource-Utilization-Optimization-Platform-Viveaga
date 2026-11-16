
package com.labresource.platform.repository;

import com.labresource.platform.entity.SharingAgreement;
import com.labresource.platform.entity.AgreementStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharingAgreementRepository
        extends JpaRepository<SharingAgreement, Long> {

    Optional<SharingAgreement> findByInstitutionSharingRequestId(Long requestId);

    List<SharingAgreement> findByOwnerInstitutionId(Long institutionId);

    List<SharingAgreement> findByPartnerInstitutionId(Long institutionId);

    List<SharingAgreement> findByStatus(AgreementStatus status);

    List<SharingAgreement> findByOwnerInstitutionIdOrPartnerInstitutionId(
            Long ownerInstitutionId,
            Long partnerInstitutionId
    );
}
