package com.labresource.platform.dto;

import java.time.LocalDateTime;

public class IdleTimeDTO {

    private Long equipmentId;
    private String equipmentName;
    private LocalDateTime lastUsedAt;
    private Double idleHours;
    private String idleStatus;

    public IdleTimeDTO(
            Long equipmentId,
            String equipmentName,
            LocalDateTime lastUsedAt,
            Double idleHours,
            String idleStatus
    ) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.lastUsedAt = lastUsedAt;
        this.idleHours = idleHours;
        this.idleStatus = idleStatus;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public Double getIdleHours() {
        return idleHours;
    }

    public String getIdleStatus() {
        return idleStatus;
    }
}