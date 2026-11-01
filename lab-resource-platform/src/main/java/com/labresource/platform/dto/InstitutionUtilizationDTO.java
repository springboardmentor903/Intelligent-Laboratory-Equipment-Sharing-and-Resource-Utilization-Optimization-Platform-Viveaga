
package com.labresource.platform.dto;

import java.math.BigDecimal;

public class InstitutionUtilizationDTO {

    private Long institutionId;
    private String institutionName;

    private BigDecimal actualUsageHours;
    private BigDecimal availableHours;
    private BigDecimal utilizationPercentage;

    public InstitutionUtilizationDTO(
            Long institutionId,
            String institutionName,
            BigDecimal actualUsageHours,
            BigDecimal availableHours,
            BigDecimal utilizationPercentage) {

        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.actualUsageHours = actualUsageHours;
        this.availableHours = availableHours;
        this.utilizationPercentage = utilizationPercentage;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
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
