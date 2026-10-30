
package com.labresource.platform.dto;

import com.labresource.platform.entity.Equipment;
import com.labresource.platform.entity.EquipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EquipmentDetailDTO {

    private Long id;
    private String name;
    private String model;
    private String manufacturer;
    private String serialNumber;
    private String assetTag;
    private String location;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private BigDecimal hourlyCost;
    private EquipmentStatus status;
    private String categoryName;
    private String departmentName;
    private String institutionName;
    private List<String> tags;
    private String calibrationStatus;

    public static EquipmentDetailDTO fromEntity(Equipment equipment) {
        EquipmentDetailDTO dto = new EquipmentDetailDTO();

        dto.id = equipment.getId();
        dto.name = equipment.getName();
        dto.model = equipment.getModel();
        dto.manufacturer = equipment.getManufacturer();
        dto.serialNumber = equipment.getSerialNumber();
        dto.assetTag = equipment.getAssetTag();
        dto.location = equipment.getLocation();
        dto.purchaseDate = equipment.getPurchaseDate();
        dto.purchaseCost = equipment.getPurchaseCost();
        dto.hourlyCost = equipment.getHourlyCost();
        dto.status = equipment.getStatus();


        if (equipment.getCategory() != null) {
            dto.categoryName = equipment.getCategory().getName();
        }

        if (equipment.getDepartment() != null) {
            dto.departmentName = equipment.getDepartment().getName();

            if (equipment.getDepartment().getInstitution() != null) {
                dto.institutionName =
                        equipment.getDepartment()
                                .getInstitution()
                                .getName();
            }
        }

        dto.tags = equipment.getTags() == null
                ? List.of()
                : equipment.getTags().stream()
                .map(tag -> tag.getName())
                .toList();

        if (equipment.getCalibrationRecords() == null
                || equipment.getCalibrationRecords().isEmpty()) {
            dto.calibrationStatus = "NOT_CALIBRATED";
        } else {
            LocalDate today = LocalDate.now();

            LocalDate nextDueDate = equipment.getCalibrationRecords()
                    .stream()
                    .map(record -> record.getNextDueDate())
                    .filter(date -> date != null)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            if (nextDueDate == null) {
                dto.calibrationStatus = "NOT_CALIBRATED";
            } else if (nextDueDate.isBefore(today)) {
                dto.calibrationStatus = "EXPIRED";
            } else if (!nextDueDate.isAfter(today.plusDays(30))) {
                dto.calibrationStatus = "DUE_SOON";
            } else {
                dto.calibrationStatus = "VALID";
            }
        }

        dto.tags = equipment.getTags() == null
                ? List.of()
                : equipment.getTags().stream()
                .map(tag -> tag.getName())
                .toList();

        return dto;

    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getModel() { return model; }
    public String getManufacturer() { return manufacturer; }
    public String getSerialNumber() { return serialNumber; }
    public String getAssetTag() { return assetTag; }
    public String getLocation() { return location; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public BigDecimal getHourlyCost() { return hourlyCost; }
    public EquipmentStatus getStatus() { return status; }
    public String getCategoryName() { return categoryName; }
    public String getDepartmentName() { return departmentName; }
    public String getInstitutionName() { return institutionName; }
    public List<String> getTags() { return tags; }
    public String getCalibrationStatus() {
        return calibrationStatus;
    }
}
