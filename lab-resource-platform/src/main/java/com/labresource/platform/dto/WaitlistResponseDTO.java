package com.labresource.platform.dto;

import com.labresource.platform.entity.Waitlist;
import com.labresource.platform.entity.WaitlistStatus;

import java.time.LocalDateTime;

public class WaitlistResponseDTO {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Long userId;
    private String userName;
    private LocalDateTime requestedStartTime;
    private LocalDateTime requestedEndTime;
    private String purpose;
    private WaitlistStatus status;
    private LocalDateTime createdAt;

    public WaitlistResponseDTO(Waitlist waitlist) {

        this.id = waitlist.getId();
        this.equipmentId = waitlist.getEquipment().getId();
        this.equipmentName = waitlist.getEquipment().getName();
        this.userId = waitlist.getUser().getId();
        this.userName = waitlist.getUser().getFullName();
        this.requestedStartTime = waitlist.getRequestedStartTime();
        this.requestedEndTime = waitlist.getRequestedEndTime();
        this.purpose = waitlist.getPurpose();
        this.status = waitlist.getStatus();
        this.createdAt = waitlist.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDateTime getRequestedStartTime() {
        return requestedStartTime;
    }

    public LocalDateTime getRequestedEndTime() {
        return requestedEndTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}