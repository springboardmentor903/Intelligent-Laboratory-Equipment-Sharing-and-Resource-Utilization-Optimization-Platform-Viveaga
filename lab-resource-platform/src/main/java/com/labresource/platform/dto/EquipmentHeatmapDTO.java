package com.labresource.platform.dto;

import java.util.Map;

public class EquipmentHeatmapDTO {

    private Long equipmentId;
    private String equipmentName;

    private Map<String, Map<String, EquipmentHeatmapSlotDTO>> hourlyUsage;

    public EquipmentHeatmapDTO(
            Long equipmentId,
            String equipmentName,
            Map<String, Map<String, EquipmentHeatmapSlotDTO>> hourlyUsage) {

        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.hourlyUsage = hourlyUsage;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public Map<String, Map<String, EquipmentHeatmapSlotDTO>> getHourlyUsage() {
        return hourlyUsage;
    }
}