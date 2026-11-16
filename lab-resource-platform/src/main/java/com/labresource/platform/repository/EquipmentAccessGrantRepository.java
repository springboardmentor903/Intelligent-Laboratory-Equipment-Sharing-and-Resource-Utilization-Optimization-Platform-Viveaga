
package com.labresource.platform.repository;

import com.labresource.platform.entity.EquipmentAccessGrant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentAccessGrantRepository
        extends JpaRepository<EquipmentAccessGrant, Long> {

    Optional<EquipmentAccessGrant> findByAgreementId(Long agreementId);

    List<EquipmentAccessGrant> findByPartnerInstitutionIdAndActiveTrue(
            Long institutionId
    );

    List<EquipmentAccessGrant> findByEquipmentIdAndActiveTrue(
            Long equipmentId
    );

    boolean existsByEquipmentIdAndPartnerInstitutionIdAndActiveTrue(
            Long equipmentId,
            Long institutionId
    );

    Optional<EquipmentAccessGrant>
    findFirstByEquipmentIdAndPartnerInstitutionIdAndActiveTrue(
            Long equipmentId,
            Long institutionId
    );
}
