
package com.labresource.platform.dto;

import com.labresource.platform.entity.UsageMode;
import java.time.LocalDateTime;

public class BookingRequestDTO {

    private Long equipmentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private UsageMode usageMode = UsageMode.EXCLUSIVE;

    private boolean recurring = false;
    private String recurrenceType;
    private Integer recurrenceCount;

    public BookingRequestDTO() {
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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

    public UsageMode getUsageMode() {
        return usageMode;
    }

    public void setUsageMode(UsageMode usageMode) {
        this.usageMode = usageMode;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public Integer getRecurrenceCount() {
        return recurrenceCount;
    }

    public void setRecurrenceCount(Integer recurrenceCount) {
        this.recurrenceCount = recurrenceCount;
    }
}
