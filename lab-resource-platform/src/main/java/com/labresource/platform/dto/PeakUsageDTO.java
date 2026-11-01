package com.labresource.platform.dto;

public class PeakUsageDTO {

    private String period;
    private Integer usageCount;
    private Boolean peakPeriod;

    public PeakUsageDTO(
            String period,
            Integer usageCount,
            Boolean peakPeriod
    ) {
        this.period = period;
        this.usageCount = usageCount;
        this.peakPeriod = peakPeriod;
    }

    public String getPeriod() {
        return period;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public Boolean getPeakPeriod() {
        return peakPeriod;
    }
}