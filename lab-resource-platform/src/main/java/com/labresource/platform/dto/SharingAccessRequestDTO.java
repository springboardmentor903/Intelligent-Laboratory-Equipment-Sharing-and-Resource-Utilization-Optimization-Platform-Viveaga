
package com.labresource.platform.dto;

public class SharingAccessRequestDTO {

    private Long equipmentSharingId;
    private String purpose;

    public Long getEquipmentSharingId() {
        return equipmentSharingId;
    }

    public void setEquipmentSharingId(Long equipmentSharingId) {
        this.equipmentSharingId = equipmentSharingId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
