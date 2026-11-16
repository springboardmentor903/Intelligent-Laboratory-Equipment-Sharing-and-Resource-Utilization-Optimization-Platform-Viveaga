
package com.labresource.platform.repository;

import com.labresource.platform.entity.ExternalBookingRequest;
import com.labresource.platform.entity.ExternalBookingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalBookingRequestRepository
        extends JpaRepository<ExternalBookingRequest, Long> {

    List<ExternalBookingRequest> findByRequestingInstitutionId(
            Long institutionId
    );

    List<ExternalBookingRequest> findByEquipmentSharingId(
            Long equipmentSharingId
    );

    List<ExternalBookingRequest> findByStatus(
            ExternalBookingRequestStatus status
    );

    List<ExternalBookingRequest> findByRequestedById(Long userId);

    List<ExternalBookingRequest>
    findByEquipmentSharingInstitutionId(Long institutionId);
}
