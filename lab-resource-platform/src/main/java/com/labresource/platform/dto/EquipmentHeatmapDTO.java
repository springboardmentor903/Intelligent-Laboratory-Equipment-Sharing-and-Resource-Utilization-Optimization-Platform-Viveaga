package com.labresource.platform.dto;

import java.util.Map;

public class EquipmentHeatmapDTO {

    private Long equipmentId;
    private String equipmentName;
    private Map<String, Integer> hourlyUsage;

    public EquipmentHeatmapDTO(
            Long equipmentId,
            String equipmentName,
            Map<String, Integer> hourlyUsage
    ) {
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

    public Map<String, Integer> getHourlyUsage() {
        return hourlyUsage;
    }
}