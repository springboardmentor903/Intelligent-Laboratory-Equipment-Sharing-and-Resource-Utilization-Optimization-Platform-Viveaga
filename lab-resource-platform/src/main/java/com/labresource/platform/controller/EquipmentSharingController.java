package com.labresource.platform.controller;

import com.labresource.platform.dto.EquipmentSharingRequestDTO;
import com.labresource.platform.dto.EquipmentSharingResponseDTO;
import com.labresource.platform.service.EquipmentSharingService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sharing")
public class EquipmentSharingController {

    private final EquipmentSharingService sharingService;

    public EquipmentSharingController(
            EquipmentSharingService sharingService
    ) {
        this.sharingService = sharingService;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentSharingResponseDTO publishEquipment(
            @RequestBody EquipmentSharingRequestDTO request,
            Authentication authentication
    ) {
        return sharingService.publishEquipment(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/available")
    public List<EquipmentSharingResponseDTO> getAvailableEquipment(
            Authentication authentication
    ) {
        return sharingService.getActiveSharedEquipment(
                authentication.getName()
        );
    }

    @GetMapping("/my-institution")
    public List<EquipmentSharingResponseDTO> getMyInstitutionSharings(
            Authentication authentication
    ) {
        return sharingService.getMyInstitutionSharings(
                authentication.getName()
        );
    }
}