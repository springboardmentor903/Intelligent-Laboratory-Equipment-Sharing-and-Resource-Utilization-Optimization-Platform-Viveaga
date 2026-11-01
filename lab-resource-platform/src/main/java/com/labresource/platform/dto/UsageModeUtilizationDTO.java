
package com.labresource.platform.dto;

import java.math.BigDecimal;

public class UsageModeUtilizationDTO {

    private Long id;
    private String name;

    private BigDecimal sharedUsageHours;
    private BigDecimal exclusiveUsageHours;

    private BigDecimal sharedUsagePercentage;
    private BigDecimal exclusiveUsagePercentage;

    public UsageModeUtilizationDTO(
            Long id,
            String name,
            BigDecimal sharedUsageHours,
            BigDecimal exclusiveUsageHours,
            BigDecimal sharedUsagePercentage,
            BigDecimal exclusiveUsagePercentage) {

        this.id = id;
        this.name = name;
        this.sharedUsageHours = sharedUsageHours;
        this.exclusiveUsageHours = exclusiveUsageHours;
        this.sharedUsagePercentage = sharedUsagePercentage;
        this.exclusiveUsagePercentage = exclusiveUsagePercentage;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSharedUsageHours() {
        return sharedUsageHours;
    }

    public BigDecimal getExclusiveUsageHours() {
        return exclusiveUsageHours;
    }

    public BigDecimal getSharedUsagePercentage() {
        return sharedUsagePercentage;
    }

    public BigDecimal getExclusiveUsagePercentage() {
        return exclusiveUsagePercentage;
    }
}
