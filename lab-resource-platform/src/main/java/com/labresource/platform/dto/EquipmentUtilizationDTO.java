
package com.labresource.platform.dto;

import java.math.BigDecimal;

public class EquipmentUtilizationDTO {

    private Long equipmentId;
    private String equipmentName;
    private String assetTag;

    private BigDecimal actualUsageHours;
    private BigDecimal availableHours;
    private BigDecimal idleHours;
    private BigDecimal utilizationPercentage;

    public EquipmentUtilizationDTO() {
    }

    public EquipmentUtilizationDTO(
            Long equipmentId,
            String equipmentName,
            String assetTag,
            BigDecimal actualUsageHours,
            BigDecimal availableHours,
            BigDecimal idleHours,
            BigDecimal utilizationPercentage) {

        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.actualUsageHours = actualUsageHours;
        this.availableHours = availableHours;
        this.idleHours = idleHours;
        this.utilizationPercentage = utilizationPercentage;
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

    public BigDecimal getActualUsageHours() {
        return actualUsageHours;
    }

    public void setActualUsageHours(BigDecimal actualUsageHours) {
        this.actualUsageHours = actualUsageHours;
    }

    public BigDecimal getAvailableHours() {
        return availableHours;
    }

    public void setAvailableHours(BigDecimal availableHours) {
        this.availableHours = availableHours;
    }

    public BigDecimal getIdleHours() {
        return idleHours;
    }

    public void setIdleHours(BigDecimal idleHours) {
        this.idleHours = idleHours;
    }

    public BigDecimal getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(BigDecimal utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }
}
