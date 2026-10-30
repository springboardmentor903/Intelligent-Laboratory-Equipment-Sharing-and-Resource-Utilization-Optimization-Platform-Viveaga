
package com.labresource.platform.dto;

import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EquipmentListDTO {

    private Long id;
    private String name;
    private String assetTag;
    private String model;
    private String manufacturer;
    private EquipmentStatus status;
    private String categoryName;
    private String departmentName;
    private BigDecimal hourlyCost;
    private LocalDate purchaseDate;

    public EquipmentListDTO() {
    }

    public static EquipmentListDTO fromEntity(Equipment equipment) {
        EquipmentListDTO dto = new EquipmentListDTO();

        dto.setId(equipment.getId());
        dto.setName(equipment.getName());
        dto.setAssetTag(equipment.getAssetTag());
        dto.setModel(equipment.getModel());
        dto.setManufacturer(equipment.getManufacturer());
        dto.setStatus(equipment.getStatus());
        dto.setHourlyCost(equipment.getHourlyCost());
        dto.setPurchaseDate(equipment.getPurchaseDate());

        if (equipment.getCategory() != null) {
            dto.setCategoryName(equipment.getCategory().getName());
        }

        if (equipment.getDepartment() != null) {
            dto.setDepartmentName(equipment.getDepartment().getName());
        }

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public BigDecimal getHourlyCost() { return hourlyCost; }
    public void setHourlyCost(BigDecimal hourlyCost) { this.hourlyCost = hourlyCost; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
}
