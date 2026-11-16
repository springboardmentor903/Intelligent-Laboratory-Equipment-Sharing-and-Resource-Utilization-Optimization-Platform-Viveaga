
package com.labresource.platform.repository;

import com.labresource.platform.entity.EquipmentSharing;
import com.labresource.platform.entity.SharingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EquipmentSharingRepository
        extends JpaRepository<EquipmentSharing, Long> {

    List<EquipmentSharing> findByStatus(SharingStatus status);

    List<EquipmentSharing> findByInstitutionId(Long institutionId);

    List<EquipmentSharing> findByEquipmentId(Long equipmentId);

    List<EquipmentSharing> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            SharingStatus status,
            LocalDate date1,
            LocalDate date2
    );
}
