
package com.labresource.platform.repository;

import com.labresource.platform.entity.CalibrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalibrationRecordRepository
        extends JpaRepository<CalibrationRecord, Long> {

    List<CalibrationRecord> findByEquipment_Id(Long equipmentId);

    List<CalibrationRecord> findByNextDueDateBefore(
            java.time.LocalDate date);
}
