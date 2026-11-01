package com.labresource.platform.dto;

import java.math.BigDecimal;

public class HistoricalBenchmarkDTO {

    private Long equipmentId;
    private String equipmentName;

    private BigDecimal currentUtilization;
    private BigDecimal previousUtilization;
    private BigDecimal utilizationChange;

    private String performanceTrend;

    public HistoricalBenchmarkDTO(
            Long equipmentId,
            String equipmentName,
            BigDecimal currentUtilization,
            BigDecimal previousUtilization,
            BigDecimal utilizationChange,
            String performanceTrend) {

        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.currentUtilization = currentUtilization;
        this.previousUtilization = previousUtilization;
        this.utilizationChange = utilizationChange;
        this.performanceTrend = performanceTrend;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public BigDecimal getCurrentUtilization() {
        return currentUtilization;
    }

    public BigDecimal getPreviousUtilization() {
        return previousUtilization;
    }

    public BigDecimal getUtilizationChange() {
        return utilizationChange;
    }

    public String getPerformanceTrend() {
        return performanceTrend;
    }
}