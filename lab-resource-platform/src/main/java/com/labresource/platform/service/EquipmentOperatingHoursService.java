
package com.labresource.platform.service;

import com.labresource.platform.dto.EquipmentOperatingHoursRequest;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentOperatingHours;
import com.labresource.platform.repository.EquipmentOperatingHoursRepository;
import com.labresource.platform.repository.EquipmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EquipmentOperatingHoursService {

    private final EquipmentOperatingHoursRepository operatingHoursRepository;
    private final EquipmentRepository equipmentRepository;

    public EquipmentOperatingHoursService(
            EquipmentOperatingHoursRepository operatingHoursRepository,
            EquipmentRepository equipmentRepository) {

        this.operatingHoursRepository = operatingHoursRepository;
        this.equipmentRepository = equipmentRepository;
    }

    // Get weekly operating hours
    @Transactional(readOnly = true)
    public List<EquipmentOperatingHours> getOperatingHours(Long equipmentId) {

        equipmentRepository.findById(equipmentId)
                .orElseThrow(() ->
                        new RuntimeException("Equipment not found"));

        List<EquipmentOperatingHours> hours =
                operatingHoursRepository
                        .findByEquipment_IdOrderByDayOfWeekAsc(equipmentId);

        if (hours.isEmpty()) {
            return createDefaultSchedule();
        }

        return hours;
    }

    // Create default Monday-Friday schedule
    private List<EquipmentOperatingHours> createDefaultSchedule() {

        return List.of(
                createDefaultDay(1),
                createDefaultDay(2),
                createDefaultDay(3),
                createDefaultDay(4),
                createDefaultDay(5)
        );
    }

    private EquipmentOperatingHours createDefaultDay(int day) {

        EquipmentOperatingHours hours = new EquipmentOperatingHours();

        hours.setDayOfWeek(day);
        hours.setOpenTime(LocalTime.of(9, 0));
        hours.setCloseTime(LocalTime.of(17, 0));

        return hours;
    }

    // Check whether equipment operates on a particular day
    public boolean isOperatingDay(
            Long equipmentId,
            DayOfWeek dayOfWeek) {

        List<EquipmentOperatingHours> hours =
                getOperatingHours(equipmentId);

        return hours.stream()
                .anyMatch(h ->
                        h.getDayOfWeek() == dayOfWeek.getValue());
    }

    // Update weekly operating hours
    @Transactional
    public List<EquipmentOperatingHours> updateOperatingHours(
            Long equipmentId,
            List<EquipmentOperatingHoursRequest> requests) {

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() ->
                        new RuntimeException("Equipment not found"));

        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException(
                    "Operating hours cannot be empty");
        }

        // Validate schedule
        Set<Integer> days = new HashSet<>();

        for (EquipmentOperatingHoursRequest request : requests) {

            if (request.getDayOfWeek() == null
                    || request.getDayOfWeek() < 1
                    || request.getDayOfWeek() > 7) {

                throw new RuntimeException(
                        "Day must be between 1 and 7");
            }

            if (!days.add(request.getDayOfWeek())) {
                throw new RuntimeException(
                        "Duplicate day in operating hours");
            }

            if (request.getOpenTime() == null
                    || request.getCloseTime() == null
                    || !request.getCloseTime()
                    .isAfter(request.getOpenTime())) {

                throw new RuntimeException(
                        "Closing time must be after opening time");
            }
        }

        // Delete existing schedule
        List<EquipmentOperatingHours> existing =
                operatingHoursRepository
                        .findByEquipment_IdOrderByDayOfWeekAsc(equipmentId);

        operatingHoursRepository.deleteAll(existing);

        // Flush deletions before inserting replacements
        operatingHoursRepository.flush();

        // Create updated schedule
        List<EquipmentOperatingHours> updated = new ArrayList<>();

        for (EquipmentOperatingHoursRequest request : requests) {

            EquipmentOperatingHours hours =
                    new EquipmentOperatingHours();

            hours.setEquipment(equipment);
            hours.setDayOfWeek(request.getDayOfWeek());
            hours.setOpenTime(request.getOpenTime());
            hours.setCloseTime(request.getCloseTime());

            updated.add(hours);
        }

        return operatingHoursRepository.saveAll(updated);
    }
}
