package com.labresource.platform.dto;

import java.math.BigDecimal;

public class PeakUsageDTO {

    private String period;
    private BigDecimal usageMinutes;
    private Boolean peakPeriod;

    public PeakUsageDTO(
            String period,
            BigDecimal usageMinutes,
            Boolean peakPeriod
    ) {
        this.period = period;
        this.usageMinutes = usageMinutes;
        this.peakPeriod = peakPeriod;
    }

    public String getPeriod() {
        return period;
    }

    public BigDecimal getUsageMinutes() {
        return usageMinutes;
    }

    public Boolean getPeakPeriod() {
        return peakPeriod;
    }
}