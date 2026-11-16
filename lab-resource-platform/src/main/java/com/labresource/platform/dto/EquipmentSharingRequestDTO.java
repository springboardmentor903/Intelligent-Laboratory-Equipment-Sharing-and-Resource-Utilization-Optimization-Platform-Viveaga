
package com.labresource.platform.dto;

import java.time.LocalDate;

public class EquipmentSharingRequestDTO {

    private Long equipmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String sharingTerms;
    private Double hourlyCost;

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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
}
