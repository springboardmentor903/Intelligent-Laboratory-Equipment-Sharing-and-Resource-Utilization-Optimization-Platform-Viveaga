package com.labresource.platform.controller;

import com.labresource.platform.dto.LiveEquipmentStatusDTO;
import com.labresource.platform.service.LiveEquipmentStatusService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live-equipment")
public class LiveEquipmentStatusController {

    private final LiveEquipmentStatusService liveEquipmentStatusService;

    public LiveEquipmentStatusController(
            LiveEquipmentStatusService liveEquipmentStatusService) {
        this.liveEquipmentStatusService = liveEquipmentStatusService;
    }

    @GetMapping
    public List<LiveEquipmentStatusDTO> getLiveEquipmentStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return liveEquipmentStatusService.getLiveEquipmentStatus(
                status, page, size
        );
    }
}