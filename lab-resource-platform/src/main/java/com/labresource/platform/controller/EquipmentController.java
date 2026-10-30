
package com.labresource.platform.controller;

import com.labresource.platform.dto.EquipmentDetailDTO;
import com.labresource.platform.dto.EquipmentListDTO;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentStatus;
import com.labresource.platform.service.EquipmentService;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    // Get equipment with pagination and filters
    @GetMapping
    public Page<EquipmentListDTO> getAllEquipment(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword) {

        return equipmentService.searchEquipment(
                page, size, status, categoryId, tagId,
                departmentId, keyword);
    }

    // Get equipment by ID
    @GetMapping("/{id}")
    public EquipmentDetailDTO getEquipmentById(@PathVariable Long id) {
        return equipmentService.getEquipmentById(id);
    }
    // Get equipment by asset tag
    @GetMapping("/asset/{assetTag}")
    public EquipmentDetailDTO getEquipmentByAssetTag(
            @PathVariable String assetTag) {
        return equipmentService.getEquipmentByAssetTag(assetTag);
    }

    // Create equipment
    @PostMapping
    public Equipment createEquipment(@RequestBody Equipment equipment) {
        return equipmentService.createEquipment(equipment);
    }


    // Update equipment
    @PutMapping("/{id}")
    public EquipmentDetailDTO updateEquipment(
            @PathVariable Long id,
            @RequestBody Equipment equipment) {
        return equipmentService.updateEquipment(id, equipment);
    }

    // Update equipment status
    @PatchMapping("/{id}/status")
    public Equipment updateEquipmentStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {

        String statusValue = body.get("status");

        if (statusValue == null || statusValue.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        EquipmentStatus status;

        try {
            status = EquipmentStatus.valueOf(statusValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + statusValue);
        }

        return equipmentService.updateEquipmentStatus(id, status);
    }



    @PutMapping("/{id}/tags")
    public EquipmentDetailDTO assignTags(
            @PathVariable Long id,
            @RequestBody java.util.Set<Long> tagIds) {

        return equipmentService.assignTags(id, tagIds);
    }

}
