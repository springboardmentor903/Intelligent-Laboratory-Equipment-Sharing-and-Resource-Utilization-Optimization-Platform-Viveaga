
package com.labresource.platform.controller;

import com.labresource.platform.dto.EquipmentOperatingHoursRequest;
import com.labresource.platform.dto.EquipmentOperatingHoursResponse;
import com.labresource.platform.entity.EquipmentOperatingHours;
import com.labresource.platform.service.EquipmentOperatingHoursService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/operating-hours")
public class EquipmentOperatingHoursController {

    private final EquipmentOperatingHoursService operatingHoursService;

    public EquipmentOperatingHoursController(
            EquipmentOperatingHoursService operatingHoursService) {
        this.operatingHoursService = operatingHoursService;
    }

    @GetMapping
    public ResponseEntity<List<EquipmentOperatingHoursResponse>> getOperatingHours(
            @PathVariable Long equipmentId) {

        List<EquipmentOperatingHoursResponse> response =
                operatingHoursService.getOperatingHours(equipmentId)
                        .stream()
                        .map(this::convertToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<List<EquipmentOperatingHoursResponse>> updateOperatingHours(
            @PathVariable Long equipmentId,
            @RequestBody List<EquipmentOperatingHoursRequest> requests) {

        List<EquipmentOperatingHoursResponse> response =
                operatingHoursService.updateOperatingHours(equipmentId, requests)
                        .stream()
                        .map(this::convertToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    private EquipmentOperatingHoursResponse convertToResponse(
            EquipmentOperatingHours hours) {

        Long equipmentId = hours.getEquipment() == null
                ? null
                : hours.getEquipment().getId();

        return new EquipmentOperatingHoursResponse(
                hours.getId(),
                equipmentId,
                hours.getDayOfWeek(),
                hours.getOpenTime(),
                hours.getCloseTime()
        );
    }
}
