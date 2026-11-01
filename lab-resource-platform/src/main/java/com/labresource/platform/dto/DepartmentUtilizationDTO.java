package com.labresource.platform.dto;

import java.math.BigDecimal;

public class DepartmentUtilizationDTO {

    private Long departmentId;
    private String departmentName;

    private BigDecimal actualUsageHours;
    private BigDecimal availableHours;
    private BigDecimal utilizationPercentage;

    public DepartmentUtilizationDTO(
            Long departmentId,
            String departmentName,
            BigDecimal actualUsageHours,
            BigDecimal availableHours,
            BigDecimal utilizationPercentage) {

        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.actualUsageHours = actualUsageHours;
        this.availableHours = availableHours;
        this.utilizationPercentage = utilizationPercentage;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public BigDecimal getActualUsageHours() {
        return actualUsageHours;
    }

    public BigDecimal getAvailableHours() {
        return availableHours;
    }

    public BigDecimal getUtilizationPercentage() {
        return utilizationPercentage;
    }
}