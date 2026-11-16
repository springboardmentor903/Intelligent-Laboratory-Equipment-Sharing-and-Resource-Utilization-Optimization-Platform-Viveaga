
package com.labresource.platform.dto;

import com.labresource.platform.entity.SharingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EquipmentSharingResponseDTO {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private Long institutionId;
    private String institutionName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String sharingTerms;
    private Double hourlyCost;
    private SharingStatus status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getSharingTerms() {
        return sharingTerms;
    }

    public void setSharingTerms(String sharingTerms) {
        this.sharingTerms = sharingTerms;
    }

    public Double getHourlyCost() {
        return hourlyCost;
    }

    public void setHourlyCost(Double hourlyCost) {
        this.hourlyCost = hourlyCost;
    }

    public SharingStatus getStatus() {
        return status;
    }

    public void setStatus(SharingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
