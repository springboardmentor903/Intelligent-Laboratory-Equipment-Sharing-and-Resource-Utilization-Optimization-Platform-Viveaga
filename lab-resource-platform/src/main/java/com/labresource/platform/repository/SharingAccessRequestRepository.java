
package com.labresource.platform.repository;

import com.labresource.platform.entity.SharingAccessRequest;
import com.labresource.platform.entity.SharingAccessRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharingAccessRequestRepository
        extends JpaRepository<SharingAccessRequest, Long> {

    List<SharingAccessRequest> findByRequestingInstitutionId(Long institutionId);

    List<SharingAccessRequest> findByEquipmentSharingId(Long equipmentSharingId);

    List<SharingAccessRequest> findByStatus(
            SharingAccessRequestStatus status
    );


    List<SharingAccessRequest> findByEquipmentSharingInstitutionId(Long institutionId);


    boolean existsByEquipmentSharingIdAndRequestingInstitutionIdAndStatus(
            Long equipmentSharingId,
            Long institutionId,
            SharingAccessRequestStatus status
    );
}
