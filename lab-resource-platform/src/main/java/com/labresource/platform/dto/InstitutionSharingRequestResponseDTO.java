
package com.labresource.platform.dto;

import com.labresource.platform.entity.AgreementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InstitutionSharingRequestResponseDTO {

    private Long id;
    private String purpose;
    private AgreementStatus status;
    private LocalDateTime requestedAt;

    private InstitutionDTO requestingInstitution;
    private EquipmentSharingDTO equipmentSharing;

    public InstitutionSharingRequestResponseDTO(
            Long id,
            String purpose,
            AgreementStatus status,
            LocalDateTime requestedAt,
            InstitutionDTO requestingInstitution,
            EquipmentSharingDTO equipmentSharing
    ) {
        this.id = id;
        this.purpose = purpose;
        this.status = status;
        this.requestedAt = requestedAt;
        this.requestingInstitution = requestingInstitution;
        this.equipmentSharing = equipmentSharing;
    }

    public Long getId() {
        return id;
    }

    public String getPurpose() {
        return purpose;
    }

    public AgreementStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public InstitutionDTO getRequestingInstitution() {
        return requestingInstitution;
    }

    public EquipmentSharingDTO getEquipmentSharing() {
        return equipmentSharing;
    }

    public static class InstitutionDTO {

        private String name;

        public InstitutionDTO(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class EquipmentSharingDTO {

        private EquipmentDTO equipment;
        private InstitutionDTO institution;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double hourlyCost;

        public EquipmentSharingDTO(
                EquipmentDTO equipment,
                InstitutionDTO institution,
                LocalDate startDate,
                LocalDate endDate,
                Double hourlyCost
        ) {
            this.equipment = equipment;
            this.institution = institution;
            this.startDate = startDate;
            this.endDate = endDate;
            this.hourlyCost = hourlyCost;
        }

        public EquipmentDTO getEquipment() {
            return equipment;
        }

        public InstitutionDTO getInstitution() {
            return institution;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public Double getHourlyCost() {
            return hourlyCost;
        }
    }

    public static class EquipmentDTO {

        private String name;
        private String assetTag;

        public EquipmentDTO(String name, String assetTag) {
            this.name = name;
            this.assetTag = assetTag;
        }

        public String getName() {
            return name;
        }

        public String getAssetTag() {
            return assetTag;
        }
    }
}
