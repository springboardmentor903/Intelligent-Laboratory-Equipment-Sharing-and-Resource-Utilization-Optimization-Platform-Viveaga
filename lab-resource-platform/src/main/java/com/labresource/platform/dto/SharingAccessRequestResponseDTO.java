
package com.labresource.platform.dto;

import com.labresource.platform.entity.SharingAccessRequestStatus;

import java.time.LocalDateTime;

public class SharingAccessRequestResponseDTO {

    private Long id;
    private Long equipmentSharingId;
    private String equipmentName;
    private Long requestingInstitutionId;
    private String requestingInstitutionName;
    private Long requestedByUserId;
    private String requestedByName;
    private String purpose;
    private SharingAccessRequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEquipmentSharingId() {
        return equipmentSharingId;
    }

    public void setEquipmentSharingId(Long equipmentSharingId) {
        this.equipmentSharingId = equipmentSharingId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public Long getRequestingInstitutionId() {
        return requestingInstitutionId;
    }

    public void setRequestingInstitutionId(Long requestingInstitutionId) {
        this.requestingInstitutionId = requestingInstitutionId;
    }

    public String getRequestingInstitutionName() {
        return requestingInstitutionName;
    }

    public void setRequestingInstitutionName(String requestingInstitutionName) {
        this.requestingInstitutionName = requestingInstitutionName;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(Long requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public SharingAccessRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SharingAccessRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }
}
