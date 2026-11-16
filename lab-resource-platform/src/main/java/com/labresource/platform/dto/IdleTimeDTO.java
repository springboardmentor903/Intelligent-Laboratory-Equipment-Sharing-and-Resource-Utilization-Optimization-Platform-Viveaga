package com.labresource.platform.dto;

import java.time.LocalDateTime;

public class IdleTimeDTO {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String departmentName;
    private Double idlePercentage;
    private Double idleHours;
    private LocalDateTime lastBookingEndedAt;

    public IdleTimeDTO(
            Long equipmentId,
            String equipmentName,
            String assetTag,
            String departmentName,
            Double idlePercentage,
            Double idleHours,
            LocalDateTime lastBookingEndedAt) {

        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.departmentName = departmentName;
        this.idlePercentage = idlePercentage;
        this.idleHours = idleHours;
        this.lastBookingEndedAt = lastBookingEndedAt;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Double getIdlePercentage() {
        return idlePercentage;
    }

    public Double getIdleHours() {
        return idleHours;
    }

    public LocalDateTime getLastBookingEndedAt() {
        return lastBookingEndedAt;
    }
}