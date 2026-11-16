
package com.labresource.platform.dto;

import java.time.LocalTime;

public class EquipmentOperatingHoursResponse {

    private Long id;
    private Long equipmentId;
    private Integer dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;

    public EquipmentOperatingHoursResponse() {
    }

    public EquipmentOperatingHoursResponse(
            Long id,
            Long equipmentId,
            Integer dayOfWeek,
            LocalTime openTime,
            LocalTime closeTime) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public Long getId() {
        return id;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}
