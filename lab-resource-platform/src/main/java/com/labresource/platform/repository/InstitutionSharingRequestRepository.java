
package com.labresource.platform.repository;

import com.labresource.platform.entity.InstitutionSharingRequest;
import com.labresource.platform.entity.AgreementStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstitutionSharingRequestRepository
        extends JpaRepository<InstitutionSharingRequest, Long> {

    List<InstitutionSharingRequest>
    findByRequestingInstitutionId(Long institutionId);

    List<InstitutionSharingRequest>
    findByEquipmentSharingInstitutionId(Long institutionId);

    List<InstitutionSharingRequest>
    findByStatus(AgreementStatus status);

    boolean existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
            Long equipmentSharingId,
            Long institutionId,
            AgreementStatus status
    );
}
