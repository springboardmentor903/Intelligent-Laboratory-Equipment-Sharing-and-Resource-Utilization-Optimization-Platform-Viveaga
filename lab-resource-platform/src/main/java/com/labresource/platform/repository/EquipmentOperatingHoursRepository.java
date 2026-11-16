package com.labresource.platform.repository;

import com.labresource.platform.entity.EquipmentOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface EquipmentOperatingHoursRepository
        extends JpaRepository<EquipmentOperatingHours, Long> {

    List<EquipmentOperatingHours> findByEquipment_IdOrderByDayOfWeekAsc(
            Long equipmentId
    );

    List<EquipmentOperatingHours> findAllByEquipment_IdIn(
            Collection<Long> equipmentIds
    );
}