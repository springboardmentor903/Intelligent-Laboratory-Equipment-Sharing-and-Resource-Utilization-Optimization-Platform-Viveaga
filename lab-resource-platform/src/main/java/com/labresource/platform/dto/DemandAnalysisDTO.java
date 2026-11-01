package com.labresource.platform.dto;

public class DemandAnalysisDTO {

    private Long equipmentId;
    private String equipmentName;
    private Long totalBookings;
    private String demandLevel;

    public DemandAnalysisDTO(
            Long equipmentId,
            String equipmentName,
            Long totalBookings,
            String demandLevel
    ) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.totalBookings = totalBookings;
        this.demandLevel = demandLevel;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public String getDemandLevel() {
        return demandLevel;
    }
}