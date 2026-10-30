
package com.labresource.platform.controller;

import com.labresource.platform.entity.CalibrationRecord;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.repository.CalibrationRecordRepository;
import com.labresource.platform.repository.EquipmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/calibrations")
public class CalibrationRecordController {

    private final CalibrationRecordRepository calibrationRepository;
    private final EquipmentRepository equipmentRepository;

    public CalibrationRecordController(
            CalibrationRecordRepository calibrationRepository,
            EquipmentRepository equipmentRepository) {
        this.calibrationRepository = calibrationRepository;
        this.equipmentRepository = equipmentRepository;
    }

    // Get calibration records for equipment
    @GetMapping
    public List<CalibrationRecord> getCalibrations(
            @PathVariable Long equipmentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new RuntimeException(
                    "Equipment not found with ID: " + equipmentId);
        }

        return calibrationRepository.findByEquipment_Id(equipmentId);
    }

    // Add calibration record
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalibrationRecord createCalibration(
            @PathVariable Long equipmentId,
            @RequestBody CalibrationRecord calibration) {

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + equipmentId));

        if (calibration.getCalibrationDate() == null
                || calibration.getNextDueDate() == null
                || calibration.getPerformedBy() == null
                || calibration.getPerformedBy().isBlank()
                || calibration.getCertificateNumber() == null
                || calibration.getCertificateNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Calibration date, next due date, performed by, "
                            + "and certificate number are required");
        }

        if (calibration.getNextDueDate()
                .isBefore(calibration.getCalibrationDate())) {
            throw new IllegalArgumentException(
                    "Next due date cannot be before calibration date");
        }

        calibration.setEquipment(equipment);

        return calibrationRepository.save(calibration);
    }
}
