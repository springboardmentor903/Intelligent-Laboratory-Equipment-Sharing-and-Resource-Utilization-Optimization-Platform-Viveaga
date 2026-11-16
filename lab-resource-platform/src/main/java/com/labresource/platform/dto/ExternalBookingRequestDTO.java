
package com.labresource.platform.dto;

import java.time.LocalDateTime;

public class ExternalBookingRequestDTO {

    private Long equipmentSharingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;

    public Long getEquipmentSharingId() {
        return equipmentSharingId;
    }

    public void setEquipmentSharingId(Long equipmentSharingId) {
        this.equipmentSharingId = equipmentSharingId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
