
package com.labresource.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "equipment_sharings",
        indexes = {
                @Index(name = "idx_sharing_equipment", columnList = "equipment_id"),
                @Index(name = "idx_sharing_institution", columnList = "institution_id")
        }
)
public class EquipmentSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Equipment being shared
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // Institution that owns the equipment
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 500)
    private String sharingTerms;

    private Double hourlyCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharingStatus status = SharingStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
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
