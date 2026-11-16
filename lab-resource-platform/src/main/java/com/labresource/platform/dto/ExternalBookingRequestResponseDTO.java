
package com.labresource.platform.dto;

import com.labresource.platform.entity.ExternalBookingRequestStatus;

import java.time.LocalDateTime;

public class ExternalBookingRequestResponseDTO {

    private Long id;
    private Long equipmentSharingId;
    private String equipmentName;
    private Long requestingInstitutionId;
    private String requestingInstitutionName;
    private Long requestedByUserId;
    private String requestedByName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private ExternalBookingRequestStatus status;
    private Long bookingId;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEquipmentSharingId() { return equipmentSharingId; }
    public void setEquipmentSharingId(Long equipmentSharingId) { this.equipmentSharingId = equipmentSharingId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public Long getRequestingInstitutionId() { return requestingInstitutionId; }
    public void setRequestingInstitutionId(Long requestingInstitutionId) { this.requestingInstitutionId = requestingInstitutionId; }

    public String getRequestingInstitutionName() { return requestingInstitutionName; }
    public void setRequestingInstitutionName(String requestingInstitutionName) { this.requestingInstitutionName = requestingInstitutionName; }

    public Long getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(Long requestedByUserId) { this.requestedByUserId = requestedByUserId; }

    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public ExternalBookingRequestStatus getStatus() { return status; }
    public void setStatus(ExternalBookingRequestStatus status) { this.status = status; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }
}
