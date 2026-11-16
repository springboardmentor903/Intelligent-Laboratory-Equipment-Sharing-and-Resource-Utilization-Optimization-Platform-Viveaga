package com.labresource.platform.dto;

import java.math.BigDecimal;

public class PeakDayHourDTO {

    private String day;
    private String hour;
    private BigDecimal usageMinutes;
    private BigDecimal utilizationPercentage;

    public PeakDayHourDTO(
            String day,
            String hour,
            BigDecimal usageMinutes,
            BigDecimal utilizationPercentage
    ) {
        this.day = day;
        this.hour = hour;
        this.usageMinutes = usageMinutes;
        this.utilizationPercentage = utilizationPercentage;
    }

    public String getDay() {
        return day;
    }

    public String getHour() {
        return hour;
    }

    public BigDecimal getUsageMinutes() {
        return usageMinutes;
    }

    public BigDecimal getUtilizationPercentage() {
        return utilizationPercentage;
    }
}