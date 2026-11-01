package com.labresource.platform.dto;

import java.time.LocalDateTime;

public class LiveEquipmentStatusDTO {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String currentStatus;
    private boolean inUse;
    private LocalDateTime actualStartTime;
    private LocalDateTime scheduledEndTime;
    private Long elapsedMinutes;
    private LocalDateTime nextBookingTime;

    public LiveEquipmentStatusDTO() {
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(LocalDateTime scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public Long getElapsedMinutes() {
        return elapsedMinutes;
    }

    public void setElapsedMinutes(Long elapsedMinutes) {
        this.elapsedMinutes = elapsedMinutes;
    }

    public LocalDateTime getNextBookingTime() {
        return nextBookingTime;
    }

    public void setNextBookingTime(LocalDateTime nextBookingTime) {
        this.nextBookingTime = nextBookingTime;
    }
}